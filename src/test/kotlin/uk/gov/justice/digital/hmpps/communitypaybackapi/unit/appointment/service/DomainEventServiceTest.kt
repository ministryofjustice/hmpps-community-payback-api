package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.appointment.service

import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.context.ApplicationEventPublisher
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.DomainEventService
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.DomainEventService.PublishDomainEventCommand
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.DomainEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.DomainEventUrlConfig
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.internal.DomainEventPublisher
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.internal.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.internal.UrlTemplate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

@ExtendWith(MockKExtension::class)
class DomainEventServiceTest {

  @MockK
  lateinit var applicationEventPublisher: ApplicationEventPublisher

  @MockK
  lateinit var domainEventUrlConfig: DomainEventUrlConfig

  @MockK
  lateinit var domainEventPublisher: DomainEventPublisher

  @InjectMockKs
  private lateinit var service: DomainEventService

  private companion object {
    val id = UUID.randomUUID()
  }

  @Nested
  inner class Publish {

    @Test
    fun `publish enqueues a spring application event`() {
      val commandEventCaptor = slot<PublishDomainEventCommand>()
      every { applicationEventPublisher.publishEvent(capture(commandEventCaptor)) } just Runs

      every { domainEventUrlConfig.domainEventDetail } returns mapOf(
        "community-payback-appointment-outcome" to UrlTemplate("http://somepath/#id"),
      )

      service.publish(
        id,
        DomainEventType.APPOINTMENT_OUTCOME,
      )

      val publishDomainEventCommand = commandEventCaptor.captured
      assertThat(publishDomainEventCommand.domainEvent.eventType).isEqualTo("community-payback.appointment.outcome")
      assertThat(publishDomainEventCommand.domainEvent.detailUrl).isEqualTo("http://somepath/$id")
      assertThat(publishDomainEventCommand.domainEvent.description).isEqualTo("A community payback appointment has been updated with an outcome")
      assertThat(publishDomainEventCommand.domainEvent.version).isEqualTo(1)
      assertThat(publishDomainEventCommand.domainEvent.occurredAt).isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.MINUTES))
    }
  }

  @Nested
  inner class PublishDomainEventCommandListener {

    @Test
    fun `pass event through to domain event publisher`() {
      val domainEvent = HmppsDomainEvent(
        eventType = "eventType",
        version = 1,
        description = "the description",
        detailUrl = "theUrl",
        occurredAt = OffsetDateTime.now(),
      )

      every { domainEventPublisher.publish(domainEvent) } just Runs

      service.publishDomainEventCommandListener(
        PublishDomainEventCommand(
          domainEvent,
        ),
      )

      verify { domainEventPublisher.publish(domainEvent) }
    }
  }
}
