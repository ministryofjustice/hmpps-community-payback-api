package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.validCreateAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.validUpdateAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdditionalInformationType
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentEventEntityFactory
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentEventService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentEventTrigger
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentValidationService.ValidatedAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.DomainEventService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.DomainEventService.EventHeaders
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.DomainEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.PersonReferenceType
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.AppointmentCreatedEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.UpdateAppointmentEvent

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

    val baselineUpdateDetails = UpdateAppointmentEvent(
      updateDto = ValidatedAppointment.validUpdateAppointment(),
      appointmentEntity = AppointmentEntity.valid(),
      existingAppointment = AppointmentDto.valid(),
      trigger = AppointmentEventTrigger.valid(),
    )

    @Test
    fun `No existing event for the appointment id, return false`() {
      every {
        appointmentEventEntityFactory.buildUpdatedEvent(baselineUpdateDetails)
      } returns AppointmentEventEntity.valid()

      every {
        appointmentEventEntityRepository.findTopByAppointmentIdOrderByCreatedAtDesc(any())
      } returns null

      val result = service.hasUpdateAlreadyBeenSent(baselineUpdateDetails)

      assertThat(result).isFalse
    }

    @Test
    fun `Latest event is creation, return false`() {
      every {
        appointmentEventEntityFactory.buildUpdatedEvent(baselineUpdateDetails)
      } returns AppointmentEventEntity.valid()

      val latestAppliedEvent = AppointmentEventEntity.valid().copy(eventType = AppointmentEventType.CREATE)
      every {
        appointmentEventEntityRepository.findTopByAppointmentIdOrderByCreatedAtDesc(any())
      } returns latestAppliedEvent

      val result = service.hasUpdateAlreadyBeenSent(baselineUpdateDetails)

      assertThat(result).isFalse
    }

    @Test
    fun `Existing update event for the appointment id, but not logically identical, return false`() {
      val latestAppliedEvent = AppointmentEventEntity.valid().copy(
        eventType = AppointmentEventType.UPDATE,
        minutesCredited = 1,
      )

      every {
        appointmentEventEntityFactory.buildUpdatedEvent(baselineUpdateDetails)
      } returns latestAppliedEvent.copy(
        eventType = AppointmentEventType.UPDATE,
        minutesCredited = 2,
      )

      every {
        appointmentEventEntityRepository.findTopByAppointmentIdOrderByCreatedAtDesc(any())
      } returns latestAppliedEvent

      val result = service.hasUpdateAlreadyBeenSent(baselineUpdateDetails)

      assertThat(result).isFalse
    }

    @Test
    fun `Existing update event for the appointment id that is logically identical, return true`() {
      val latestAppliedEvent = AppointmentEventEntity.valid().copy(eventType = AppointmentEventType.UPDATE)

      every {
        appointmentEventEntityFactory.buildUpdatedEvent(baselineUpdateDetails)
      } returns latestAppliedEvent

      every {
        appointmentEventEntityRepository.findTopByAppointmentIdOrderByCreatedAtDesc(any())
      } returns latestAppliedEvent

      val result = service.hasUpdateAlreadyBeenSent(baselineUpdateDetails)

      assertThat(result).isTrue
    }
  }

  @Nested
  inner class PublishCreateEventOnTransactionCommit {

    @Test
    fun success() {
      val appointmentEntity = AppointmentEntity.valid().copy(
        deliusId = 52L,
        crn = "CRN1",
      )

      val createDetails = AppointmentCreatedEvent(
        appointmentEntity = AppointmentEntity.valid(),
        trigger = AppointmentEventTrigger.valid(),
        createDto = ValidatedAppointment.validCreateAppointment(),
      )

      val createEvent = AppointmentEventEntity.valid().copy(
        eventType = AppointmentEventType.CREATE,
        appointment = appointmentEntity,
      )
      every {
        appointmentEventEntityFactory.buildCreatedEvent(createDetails)
      } returns createEvent

      every {
        appointmentEventEntityRepository.save(createEvent)
      } returnsArgument 0

      service.persistAndPublishAppointmentCreatedDomainEvent(createDetails)

      verify {
        domainEventService.publishOnTransactionCommit(
          id = createEvent.id,
          type = DomainEventType.APPOINTMENT_CREATED,
          headers = EventHeaders(
            additionalInformation = mapOf(
              AdditionalInformationType.APPOINTMENT_ID to appointmentEntity.id,
              AdditionalInformationType.DELIUS_APPOINTMENT_ID to 52L,
            ),
            personReferences = mapOf(PersonReferenceType.CRN to "CRN1"),
          ),
        )
      }
    }
  }

  @Nested
  inner class PublishUpdateEventOnTransactionCommit {

    @Test
    fun success() {
      val appointmentEntity = AppointmentEntity.valid().copy(
        deliusId = 52L,
        crn = "CRN1",
      )

      val updateDetails = UpdateAppointmentEvent(
        updateDto = ValidatedAppointment.validUpdateAppointment(),
        appointmentEntity = AppointmentEntity.valid(),
        existingAppointment = AppointmentDto.valid(),
        trigger = AppointmentEventTrigger.valid(),
      )

      val updateEvent = AppointmentEventEntity.valid().copy(
        eventType = AppointmentEventType.UPDATE,
        appointment = appointmentEntity,
      )

      every { appointmentEventEntityFactory.buildUpdatedEvent(updateDetails) } returns updateEvent

      every {
        appointmentEventEntityRepository.save(updateEvent)
      } returnsArgument 0

      service.persistAndPublishAppointmentUpdateDomainEvent(updateDetails)

      verify {
        domainEventService.publishOnTransactionCommit(
          id = updateEvent.id,
          type = DomainEventType.APPOINTMENT_UPDATED,
          headers = EventHeaders(
            additionalInformation = mapOf(
              AdditionalInformationType.APPOINTMENT_ID to appointmentEntity.id,
              AdditionalInformationType.DELIUS_APPOINTMENT_ID to 52L,
            ),
            personReferences = mapOf(PersonReferenceType.CRN to "CRN1"),
          ),
        )
      }
    }
  }
}
