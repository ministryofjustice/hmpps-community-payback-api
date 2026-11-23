package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.event.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.NDeliusRollbackService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.RequestScopedEvents
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.AdjustmentCreatedEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.AdjustmentCreationRequested
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.NDeliusRollbackRequired
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.SpringEventPublisher
import java.util.UUID

class NDeliusRollbackServiceTest {
  private val publisher = mockk<SpringEventPublisher>(relaxed = true)
  private val context = mockk<ApplicationContext>()
  private val events = RequestScopedEvents()
  private val service = NDeliusRollbackService(publisher, mockk(relaxed = true), context)

  init {
    every { context.getBean<RequestScopedEvents>() } returns events
  }

  @Test
  fun `compensates a retained reference without a created event`() {
    val pending = AdjustmentCreationRequested(UUID.randomUUID())
    service.captureEvent(pending)

    service.publishEventsForRollback()

    verify(exactly = 1) { publisher.publishEvent(NDeliusRollbackRequired(pending)) }
  }

  @Test
  fun `completed creations are compensated once and pending references do not inflate the safety limit`() {
    val created = List(26) { AdjustmentCreatedEvent.valid() }
    created.forEach {
      service.captureEvent(AdjustmentCreationRequested(it.adjustmentEntity.id))
      service.captureEvent(it)
    }

    service.publishEventsForRollback()

    created.forEach {
      verify(exactly = 1) { publisher.publishEvent(NDeliusRollbackRequired(it)) }
      verify(exactly = 0) { publisher.publishEvent(NDeliusRollbackRequired(AdjustmentCreationRequested(it.adjustmentEntity.id))) }
    }
  }
}
