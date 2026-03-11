package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdditionalInformationType
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentEventEntityFactory
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentEventService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.DomainEventService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.DomainEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.PersonReferenceType

@ExtendWith(MockKExtension::class)
class AppointmentEventServiceTest {

  @RelaxedMockK
  lateinit var appointmentEventEntityRepository: AppointmentEventEntityRepository

  @RelaxedMockK
  lateinit var appointmentEventEntityFactory: AppointmentEventEntityFactory

  @RelaxedMockK
  lateinit var domainEventService: DomainEventService

  @InjectMockKs
  private lateinit var service: AppointmentEventService

  @Nested
  inner class HasUpdateAlreadyBeenSent {

    @Test
    fun `Error if proposed update is a create event`() {
      assertThatThrownBy {
        service.hasUpdateAlreadyBeenSent(
          proposedUpdate = AppointmentEventEntity.valid().copy(eventType = AppointmentEventType.CREATE),
        )
      }.hasMessage("Can only check if an update has already been sent for events of type UPDATE")
    }

    @Test
    fun `No existing event for the appointment id, return false`() {
      every {
        appointmentEventEntityRepository.findTopByDeliusAppointmentIdOrderByCreatedAtDesc(any())
      } returns null

      val result = service.hasUpdateAlreadyBeenSent(
        proposedUpdate = AppointmentEventEntity.valid().copy(eventType = AppointmentEventType.UPDATE),
      )

      assertThat(result).isFalse
    }

    @Test
    fun `Latest event is creation, return false`() {
      val latestAppliedEvent = AppointmentEventEntity.valid().copy(eventType = AppointmentEventType.CREATE)

      every {
        appointmentEventEntityRepository.findTopByDeliusAppointmentIdOrderByCreatedAtDesc(any())
      } returns latestAppliedEvent

      val result = service.hasUpdateAlreadyBeenSent(
        proposedUpdate = latestAppliedEvent.copy(eventType = AppointmentEventType.UPDATE),
      )

      assertThat(result).isFalse
    }

    @Test
    fun `Existing update event for the appointment id, but not logically identical, return false`() {
      val latestAppliedEvent = AppointmentEventEntity.valid().copy(
        eventType = AppointmentEventType.UPDATE,
        minutesCredited = 1,
      )

      every {
        appointmentEventEntityRepository.findTopByDeliusAppointmentIdOrderByCreatedAtDesc(any())
      } returns latestAppliedEvent

      val result = service.hasUpdateAlreadyBeenSent(
        proposedUpdate = latestAppliedEvent.copy(
          eventType = AppointmentEventType.UPDATE,
          minutesCredited = 2,
        ),
      )

      assertThat(result).isFalse
    }

    @Test
    fun `Existing update event for the appointment id that is logically identical, return true`() {
      val latestAppliedEvent = AppointmentEventEntity.valid().copy(eventType = AppointmentEventType.UPDATE)

      every {
        appointmentEventEntityRepository.findTopByDeliusAppointmentIdOrderByCreatedAtDesc(any())
      } returns latestAppliedEvent

      val result = service.hasUpdateAlreadyBeenSent(
        proposedUpdate = latestAppliedEvent.copy(eventType = AppointmentEventType.UPDATE),
      )

      assertThat(result).isTrue
    }
  }

  @Nested
  inner class SaveAndPublishOnTransactionCommit {

    @Test
    fun success() {
      val createEvent = AppointmentEventEntity.valid().copy(
        eventType = AppointmentEventType.CREATE,
        deliusAppointmentId = 52L,
        crn = "CRN1",
      )

      val updateEvent = AppointmentEventEntity.valid().copy(
        eventType = AppointmentEventType.UPDATE,
        deliusAppointmentId = 53L,
        crn = "CRN2",
      )

      every {
        appointmentEventEntityRepository.saveAll(listOf(createEvent, updateEvent))
      } returnsArgument 0

      service.saveAndPublishOnTransactionCommit(
        listOf(createEvent, updateEvent),
      )

      verify {
        appointmentEventEntityRepository.saveAll(listOf(createEvent, updateEvent))

        domainEventService.publishOnTransactionCommit(
          id = createEvent.id,
          type = DomainEventType.APPOINTMENT_CREATED,
          additionalInformation = mapOf(AdditionalInformationType.APPOINTMENT_ID to 52L),
          personReferences = mapOf(PersonReferenceType.CRN to "CRN1"),
        )

        domainEventService.publishOnTransactionCommit(
          id = updateEvent.id,
          type = DomainEventType.APPOINTMENT_UPDATED,
          additionalInformation = mapOf(AdditionalInformationType.APPOINTMENT_ID to 53L),
          personReferences = mapOf(PersonReferenceType.CRN to "CRN2"),
        )
      }
    }
  }
}
