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
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdditionalInformationType
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentEventEntityFactory
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentEventService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentEventService.EventCreationOutcome
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentEventTrigger
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.DomainEventService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.DomainEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.PersonReferenceType

@ExtendWith(MockKExtension::class)
class AppointmentEventServiceTest {
  @RelaxedMockK
  lateinit var appointmentEventEntityFactory: AppointmentEventEntityFactory

  @RelaxedMockK
  lateinit var appointmentEventEntityRepository: AppointmentEventEntityRepository

  @RelaxedMockK
  lateinit var domainEventService: DomainEventService

  @InjectMockKs
  private lateinit var service: AppointmentEventService

  private companion object {
    const val PROJECT_CODE = "PROJ123"
    const val APPOINTMENT_ID = 101L
    val TRIGGER: AppointmentEventTrigger = AppointmentEventTrigger(AppointmentEventTriggerType.USER, "user1")
  }

  @Nested
  inner class UpdateAppointmentEvent {

    val existingAppointment = AppointmentDto.valid()
    val updateRequest = UpdateAppointmentOutcomeDto.valid().copy(deliusId = APPOINTMENT_ID)

    @Test
    fun `if there's no existing event for the appointment ids, persist new event and raise domain event`() {
      every { appointmentEventEntityRepository.findTopByDeliusAppointmentIdOrderByCreatedAtDesc(APPOINTMENT_ID) } returns null

      val entityReturnedByFactory = AppointmentEventEntity.fromUpdateRequest(updateRequest)
      every { appointmentEventEntityFactory.buildUpdatedEvent(updateRequest, existingAppointment, TRIGGER) } returns entityReturnedByFactory
      every { appointmentEventEntityRepository.save(any()) } returnsArgument 0

      val outcome = service.createUpdateEvent(
        validatedUpdate = updateRequest,
        trigger = TRIGGER,
        existingAppointment = existingAppointment,
      )

      assertThat(outcome).isEqualTo(EventCreationOutcome.EventCreated(entityReturnedByFactory))

      verify {
        appointmentEventEntityRepository.save(entityReturnedByFactory)
        domainEventService.publishOnTransactionCommit(
          id = entityReturnedByFactory.id,
          type = DomainEventType.APPOINTMENT_UPDATED,
          additionalInformation = mapOf(AdditionalInformationType.APPOINTMENT_ID to updateRequest.deliusId),
          personReferences = mapOf(PersonReferenceType.CRN to existingAppointment.offender.crn),
        )
      }
    }

    @Test
    fun `if there's an existing event for the appointment id and it's logically identical, return DuplicateIgnored`() {
      val existingIdenticalEntity = AppointmentEventEntity.fromUpdateRequest(updateRequest)

      every { appointmentEventEntityRepository.findTopByDeliusAppointmentIdOrderByCreatedAtDesc(APPOINTMENT_ID) } returns existingIdenticalEntity
      every { appointmentEventEntityFactory.buildUpdatedEvent(updateRequest, existingAppointment, TRIGGER) } returns existingIdenticalEntity

      val outcome = service.createUpdateEvent(
        validatedUpdate = updateRequest,
        trigger = TRIGGER,
        existingAppointment = existingAppointment,
      )

      assertThat(outcome).isEqualTo(EventCreationOutcome.DuplicateIgnored)

      verify(exactly = 0) {
        appointmentEventEntityRepository.save(any())
      }
    }

    @Test
    fun `if there's an existing event for the appointment id but it's not logically identical, persist new event and raise domain event`() {
      val existingOutcomeEntity = AppointmentEventEntity.fromUpdateRequest(updateRequest)

      every { appointmentEventEntityRepository.findTopByDeliusAppointmentIdOrderByCreatedAtDesc(APPOINTMENT_ID) } returns existingOutcomeEntity
      val entityReturnedByFactory = AppointmentEventEntity.fromUpdateRequest(updateRequest).copy(deliusAppointmentId = APPOINTMENT_ID)
      every { appointmentEventEntityFactory.buildUpdatedEvent(updateRequest, existingAppointment, TRIGGER) } returns entityReturnedByFactory
      every { appointmentEventEntityRepository.save(any()) } returnsArgument 0

      val outcome = service.createUpdateEvent(
        validatedUpdate = updateRequest,
        trigger = TRIGGER,
        existingAppointment = existingAppointment,
      )

      assertThat(outcome).isEqualTo(EventCreationOutcome.EventCreated(entityReturnedByFactory))

      verify {
        appointmentEventEntityRepository.save(entityReturnedByFactory)
        domainEventService.publishOnTransactionCommit(
          id = entityReturnedByFactory.id,
          type = DomainEventType.APPOINTMENT_UPDATED,
          additionalInformation = mapOf(AdditionalInformationType.APPOINTMENT_ID to updateRequest.deliusId),
          personReferences = mapOf(PersonReferenceType.CRN to existingAppointment.offender.crn),
        )
      }
    }
  }

  fun AppointmentEventEntity.Companion.fromUpdateRequest(updateRequest: UpdateAppointmentOutcomeDto) = AppointmentEventEntity.valid()
    .copy(
      eventType = AppointmentEventType.UPDATE,
      deliusAppointmentId = updateRequest.deliusId,
      priorDeliusVersion = updateRequest.deliusVersionToUpdate,
    )
}
