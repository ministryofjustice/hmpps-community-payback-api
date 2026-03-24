package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.ConflictException
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.InternalServerErrorException
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentEventService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentEventTrigger
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentRetrievalService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentUpdateService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentValidationService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.Validated
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.ToAppointmentEntity.toAppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.unit.util.WebClientResponseExceptionFactory

@ExtendWith(MockKExtension::class)
class AppointmentUpdateServiceTest {

  @RelaxedMockK
  lateinit var appointmentRetrievalService: AppointmentRetrievalService

  @RelaxedMockK
  lateinit var appointmentEventService: AppointmentEventService

  @RelaxedMockK
  lateinit var communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient

  @RelaxedMockK
  lateinit var appointmentOutcomeValidationService: AppointmentValidationService

  @RelaxedMockK
  lateinit var appointmentEntityRepository: AppointmentEntityRepository

  @InjectMockKs
  lateinit var service: AppointmentUpdateService

  companion object {
    const val PROJECT_CODE = "PROJ123"
    const val DELIUS_APPOINTMENT_ID = 101L
    val TRIGGER = AppointmentEventTrigger.valid()
  }

  @Nested
  inner class UpdateAppointmentOutcome {

    val existingAppointment = AppointmentDto.valid().copy(id = DELIUS_APPOINTMENT_ID)
    val updateRequest = UpdateAppointmentOutcomeDto.valid().copy(deliusId = DELIUS_APPOINTMENT_ID)

    @Test
    fun `if appointment not found on retrieval, pass through exception`() {
      val upstreamException = NotFoundException("not found!")
      every { appointmentRetrievalService.getAppointment(PROJECT_CODE, DELIUS_APPOINTMENT_ID) } throws upstreamException

      assertThatThrownBy {
        service.updateAppointmentOutcome(
          projectCode = PROJECT_CODE,
          update = updateRequest,
          trigger = TRIGGER,
        )
      }.isSameAs(upstreamException)
    }

    @Test
    fun `if appointment not found on update, throw not found exception`() {
      every { appointmentRetrievalService.getAppointment(PROJECT_CODE, DELIUS_APPOINTMENT_ID) } returns existingAppointment
      val proposedEvent = AppointmentEventEntity.fromUpdateRequest(updateRequest)
      every { appointmentEventService.buildUpdatedEvent(any(), any(), any(), any()) } returns proposedEvent
      every { appointmentEventService.hasUpdateAlreadyBeenSent(proposedEvent) } returns false
      every { appointmentEventService.saveAndPublishOnTransactionCommit(proposedEvent) } returnsArgument 0

      every {
        communityPaybackAndDeliusClient.updateAppointment(any(), any(), any())
      } throws WebClientResponseExceptionFactory.notFound()

      assertThatThrownBy {
        service.updateAppointmentOutcome(
          projectCode = PROJECT_CODE,
          update = updateRequest,
          trigger = TRIGGER,
        )
      }.isInstanceOf(NotFoundException::class.java).hasMessage("Appointment not found for ID '$DELIUS_APPOINTMENT_ID'")
    }

    @Test
    fun `if there's no existing entries for the delius appointment ids, persist new entry, raise domain event and invoke update endpoint`() {
      every { appointmentEntityRepository.findByDeliusId(DELIUS_APPOINTMENT_ID) } returns null
      every { appointmentEntityRepository.save(existingAppointment.toAppointmentEntity()) } returns existingAppointment.toAppointmentEntity()
      every { appointmentRetrievalService.getAppointment(PROJECT_CODE, DELIUS_APPOINTMENT_ID) } returns existingAppointment
      every { appointmentOutcomeValidationService.validateUpdate(any(), any()) } returns Validated(updateRequest)

      val proposedEvent = AppointmentEventEntity.fromUpdateRequest(updateRequest)
      every { appointmentEventService.buildUpdatedEvent(Validated(updateRequest), existingAppointment, TRIGGER, PROJECT_CODE) } returns proposedEvent
      every { appointmentEventService.hasUpdateAlreadyBeenSent(proposedEvent) } returns false
      every { appointmentEventService.saveAndPublishOnTransactionCommit(proposedEvent) } returnsArgument 0

      service.updateAppointmentOutcome(
        projectCode = PROJECT_CODE,
        update = updateRequest,
        trigger = TRIGGER,
      )

      verify {
        appointmentEntityRepository.save(existingAppointment.toAppointmentEntity())
        appointmentEventService.saveAndPublishOnTransactionCommit(proposedEvent)
        communityPaybackAndDeliusClient.updateAppointment(
          projectCode = PROJECT_CODE,
          appointmentId = DELIUS_APPOINTMENT_ID,
          updateAppointment = any(),
        )
      }
    }

    @Test
    fun `if there's an existing entry for the delius appointment id and it's logically identical, do not send an update`() {
      every { appointmentRetrievalService.getAppointment(PROJECT_CODE, DELIUS_APPOINTMENT_ID) } returns existingAppointment
      every { appointmentOutcomeValidationService.validateUpdate(any(), any()) } returns Validated(updateRequest)

      val proposedEvent = AppointmentEventEntity.fromUpdateRequest(updateRequest)
      every { appointmentEventService.buildUpdatedEvent(Validated(updateRequest), existingAppointment, TRIGGER, PROJECT_CODE) } returns proposedEvent
      every { appointmentEventService.hasUpdateAlreadyBeenSent(proposedEvent) } returns true

      service.updateAppointmentOutcome(
        projectCode = PROJECT_CODE,
        update = updateRequest,
        trigger = TRIGGER,
      )

      verify(exactly = 0) {
        appointmentEventService.saveAndPublishOnTransactionCommit(proposedEvent)
        communityPaybackAndDeliusClient.updateAppointment(any(), any(), any())
      }
    }

    @Test
    fun `if appointment has newer version on update, throw conflict exception`() {
      every { appointmentRetrievalService.getAppointment(PROJECT_CODE, DELIUS_APPOINTMENT_ID) } returns existingAppointment
      every { appointmentOutcomeValidationService.validateUpdate(any(), any()) } returns Validated(updateRequest)

      val proposedEvent = AppointmentEventEntity.fromUpdateRequest(updateRequest)
      every { appointmentEventService.buildUpdatedEvent(any(), any(), any(), any()) } returns proposedEvent
      every { appointmentEventService.hasUpdateAlreadyBeenSent(proposedEvent) } returns false
      every { appointmentEventService.saveAndPublishOnTransactionCommit(proposedEvent) } returnsArgument 0

      every {
        communityPaybackAndDeliusClient.updateAppointment(any(), any(), any())
      } throws WebClientResponseExceptionFactory.conflict()

      assertThatThrownBy {
        service.updateAppointmentOutcome(
          projectCode = PROJECT_CODE,
          update = updateRequest,
          trigger = TRIGGER,
        )
      }.isInstanceOf(ConflictException::class.java).hasMessage("A newer version of the appointment exists. Stale version is '${updateRequest.deliusVersionToUpdate}'")
    }

    @Test
    fun `if bad request returned throw internal server error`() {
      every { appointmentRetrievalService.getAppointment(PROJECT_CODE, DELIUS_APPOINTMENT_ID) } returns existingAppointment
      every { appointmentOutcomeValidationService.validateUpdate(any(), any()) } returns Validated(updateRequest)

      val proposedEvent = AppointmentEventEntity.fromUpdateRequest(updateRequest)
      every { appointmentEventService.buildUpdatedEvent(any(), any(), any(), any()) } returns proposedEvent
      every { appointmentEventService.hasUpdateAlreadyBeenSent(proposedEvent) } returns false
      every { appointmentEventService.saveAndPublishOnTransactionCommit(proposedEvent) } returnsArgument 0

      every {
        communityPaybackAndDeliusClient.updateAppointment(any(), any(), any())
      } throws WebClientResponseExceptionFactory.badRequest("didn't look good")

      assertThatThrownBy {
        service.updateAppointmentOutcome(
          projectCode = PROJECT_CODE,
          update = updateRequest,
          trigger = TRIGGER,
        )
      }.isInstanceOf(InternalServerErrorException::class.java).hasMessage("Bad request returned updating an appointment. Upstream response is 'didn't look good'")
    }
  }

  fun AppointmentEventEntity.Companion.fromUpdateRequest(updateRequest: UpdateAppointmentOutcomeDto) = AppointmentEventEntity.valid()
    .copy(
      eventType = AppointmentEventType.UPDATE,
      deliusAppointmentId = updateRequest.deliusId,
      priorDeliusVersion = updateRequest.deliusVersionToUpdate,
    )
}
