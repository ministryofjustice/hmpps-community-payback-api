package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

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
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdditionalInformationType
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.DomainEventService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.DomainEventService.PublishDomainEventCommand
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.DomainEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.DomainEventUrlConfig
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.PersonReferenceType
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.DomainEventPublisher
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.HmmpsEventPersonReference
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.UrlTemplate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.Map.entry
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
    val id: UUID = UUID.randomUUID()
  }

  @Nested
  inner class PublishOnTransactionCommit {

    @Test
    fun `enqueues a spring application event containing a fully populated HmppsDomainEvent`() {
      val commandEventCaptor = slot<PublishDomainEventCommand>()
      every { applicationEventPublisher.publishEvent(capture(commandEventCaptor)) } just Runs

      every { domainEventUrlConfig.domainEventDetail } returns mapOf(
        "appointment_updated" to UrlTemplate("http://somepath/#id"),
      )

      service.publishOnTransactionCommit(
        id = id,
        type = DomainEventType.APPOINTMENT_UPDATED,
        additionalInformation = mapOf(AdditionalInformationType.APPOINTMENT_ID to "the appointment id"),
        personReferences = mapOf(PersonReferenceType.CRN to "CRN1"),
      )

      val publishDomainEventCommand = commandEventCaptor.captured
      assertThat(publishDomainEventCommand.domainEvent.eventType).isEqualTo("community-payback.appointment.updated")
      assertThat(publishDomainEventCommand.domainEvent.detailUrl).isEqualTo("http://somepath/$id")
      assertThat(publishDomainEventCommand.domainEvent.description).isEqualTo("A community payback appointment has been updated")
      assertThat(publishDomainEventCommand.domainEvent.version).isEqualTo(1)
      assertThat(publishDomainEventCommand.domainEvent.occurredAt).isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.MINUTES))
      assertThat(publishDomainEventCommand.domainEvent.additionalInformation!!.map).containsExactly(entry("APPOINTMENT_ID", "the appointment id"))
      assertThat(publishDomainEventCommand.domainEvent.personReference!!.identifiers).containsExactly(
        HmmpsEventPersonReference("CRN", "CRN1"),
      )
    }

    @Test
    fun `dont populate additional information if no values`() {
      val commandEventCaptor = slot<PublishDomainEventCommand>()
      every { applicationEventPublisher.publishEvent(capture(commandEventCaptor)) } just Runs

      every { domainEventUrlConfig.domainEventDetail } returns mapOf(
        "appointment_updated" to UrlTemplate("http://somepath/#id"),
      )

      service.publishOnTransactionCommit(
        id = id,
        type = DomainEventType.APPOINTMENT_UPDATED,
        additionalInformation = emptyMap(),
      )

      val publishDomainEventCommand = commandEventCaptor.captured
      assertThat(publishDomainEventCommand.domainEvent.additionalInformation).isNull()
    }

    @Test
    fun `dont populate person reference if no values`() {
      val commandEventCaptor = slot<PublishDomainEventCommand>()
      every { applicationEventPublisher.publishEvent(capture(commandEventCaptor)) } just Runs

      every { domainEventUrlConfig.domainEventDetail } returns mapOf(
        "appointment_updated" to UrlTemplate("http://somepath/#id"),
      )

      service.publishOnTransactionCommit(
        id = id,
        type = DomainEventType.APPOINTMENT_UPDATED,
        personReferences = emptyMap(),
      )

      val publishDomainEventCommand = commandEventCaptor.captured
      assertThat(publishDomainEventCommand.domainEvent.personReference).isNull()
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
