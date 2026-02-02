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
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.FormKeyDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.ConflictException
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.InternalServerErrorException
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdditionalInformationType
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentEventEntityFactory
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentEventTrigger
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentRetrievalService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentUpdateService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentUpdateValidationService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.DomainEventService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.DomainEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.FormService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.PersonReferenceType
import uk.gov.justice.digital.hmpps.communitypaybackapi.unit.util.WebClientResponseExceptionFactory

@SuppressWarnings("UnusedPrivateProperty")
@ExtendWith(MockKExtension::class)
class AppointmentUpdateServiceTest {

  @RelaxedMockK
  lateinit var appointmentRetrievalService: AppointmentRetrievalService

  @RelaxedMockK
  lateinit var appointmentEventEntityRepository: AppointmentEventEntityRepository

  @RelaxedMockK
  lateinit var communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient

  @RelaxedMockK
  lateinit var formService: FormService

  @RelaxedMockK
  private lateinit var appointmentOutcomeValidationService: AppointmentUpdateValidationService

  @RelaxedMockK
  private lateinit var appointmentEventEntityFactory: AppointmentEventEntityFactory

  @RelaxedMockK
  private lateinit var domainEventService: DomainEventService

  @InjectMockKs
  private lateinit var service: AppointmentUpdateService

  private companion object {
    const val PROJECT_CODE = "PROJ123"
    const val APPOINTMENT_ID = 101L
    val TRIGGER: AppointmentEventTrigger = AppointmentEventTrigger(AppointmentEventTriggerType.USER, "user1")
  }

  @Nested
  inner class UpdateAppointmentOutcome {

    val existingAppointment = AppointmentDto.valid()
    val updateRequest = UpdateAppointmentOutcomeDto.valid().copy(deliusId = APPOINTMENT_ID)

    @Test
    fun `if appointment not found on retrieval, pass through exception`() {
      val upstreamException = NotFoundException("not found!")
      every { appointmentRetrievalService.getAppointment(PROJECT_CODE, APPOINTMENT_ID) } throws upstreamException

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
      every { appointmentRetrievalService.getAppointment(PROJECT_CODE, APPOINTMENT_ID) } returns existingAppointment
      every { appointmentEventEntityRepository.findTopByDeliusAppointmentIdOrderByCreatedAtDesc(APPOINTMENT_ID) } returns null
      every { appointmentEventEntityFactory.buildUpdatedEvent(any(), any(), any()) } returns AppointmentEventEntity.fromUpdateRequest(updateRequest)
      every { appointmentEventEntityRepository.save(any()) } returnsArgument 0

      every {
        communityPaybackAndDeliusClient.updateAppointment(any(), any(), any())
      } throws WebClientResponseExceptionFactory.notFound()

      assertThatThrownBy {
        service.updateAppointmentOutcome(
          projectCode = PROJECT_CODE,
          update = updateRequest,
          trigger = TRIGGER,
        )
      }.isInstanceOf(NotFoundException::class.java).hasMessage("Appointment not found for ID '$APPOINTMENT_ID'")
    }

    @Test
    fun `if there's no existing entries for the delius appointment ids, persist new entry, raise domain event and invoke update endpoint`() {
      every { appointmentRetrievalService.getAppointment(PROJECT_CODE, APPOINTMENT_ID) } returns existingAppointment
      every { appointmentEventEntityRepository.findTopByDeliusAppointmentIdOrderByCreatedAtDesc(APPOINTMENT_ID) } returns null

      val entityReturnedByFactory = AppointmentEventEntity.fromUpdateRequest(updateRequest)
      every { appointmentEventEntityFactory.buildUpdatedEvent(updateRequest, existingAppointment, TRIGGER) } returns entityReturnedByFactory
      every { appointmentEventEntityRepository.save(any()) } returnsArgument 0

      service.updateAppointmentOutcome(
        projectCode = PROJECT_CODE,
        update = updateRequest,
        trigger = TRIGGER,
      )

      verify {
        appointmentEventEntityRepository.save(entityReturnedByFactory)
        domainEventService.publishOnTransactionCommit(
          id = entityReturnedByFactory.id,
          type = DomainEventType.APPOINTMENT_UPDATED,
          additionalInformation = mapOf(AdditionalInformationType.APPOINTMENT_ID to updateRequest.deliusId),
          personReferences = mapOf(PersonReferenceType.CRN to existingAppointment.offender.crn),
        )
        communityPaybackAndDeliusClient.updateAppointment(
          projectCode = PROJECT_CODE,
          appointmentId = APPOINTMENT_ID,
          updateAppointment = any(),
        )
      }
    }

    @Test
    fun `if there's an existing entry for the delius appointment id and it's logically identical, do send an update`() {
      val existingIdenticalEntity = AppointmentEventEntity.fromUpdateRequest(updateRequest)

      every { appointmentRetrievalService.getAppointment(PROJECT_CODE, APPOINTMENT_ID) } returns existingAppointment
      every { appointmentEventEntityRepository.findTopByDeliusAppointmentIdOrderByCreatedAtDesc(APPOINTMENT_ID) } returns existingIdenticalEntity
      every { appointmentEventEntityFactory.buildUpdatedEvent(updateRequest, existingAppointment, TRIGGER) } returns existingIdenticalEntity

      service.updateAppointmentOutcome(
        projectCode = PROJECT_CODE,
        update = updateRequest,
        trigger = TRIGGER,
      )

      verify(exactly = 0) {
        appointmentEventEntityRepository.save(any())
        communityPaybackAndDeliusClient.updateAppointment(any(), any(), any())
      }
    }

    @Test
    fun `if there's an existing entry for the delius appointment id but it's not logically identical, persist new entry send an update`() {
      val existingOutcomeEntity = AppointmentEventEntity.fromUpdateRequest(updateRequest)

      every { appointmentRetrievalService.getAppointment(PROJECT_CODE, APPOINTMENT_ID) } returns existingAppointment
      every { appointmentEventEntityRepository.findTopByDeliusAppointmentIdOrderByCreatedAtDesc(APPOINTMENT_ID) } returns existingOutcomeEntity
      every { appointmentEventEntityFactory.buildUpdatedEvent(updateRequest, existingAppointment, TRIGGER) } returns AppointmentEventEntity.fromUpdateRequest(updateRequest).copy(deliusAppointmentId = APPOINTMENT_ID)
      every { appointmentEventEntityRepository.save(any()) } returnsArgument 0

      service.updateAppointmentOutcome(
        projectCode = PROJECT_CODE,
        update = updateRequest,
        trigger = TRIGGER,
      )

      verify { appointmentEventEntityRepository.save(any()) }
      verify { communityPaybackAndDeliusClient.updateAppointment(PROJECT_CODE, APPOINTMENT_ID, any()) }
    }

    @Test
    fun `if appointment has newer version on update, throw conflict exception`() {
      every { appointmentRetrievalService.getAppointment(PROJECT_CODE, APPOINTMENT_ID) } returns existingAppointment
      every { appointmentEventEntityRepository.findTopByDeliusAppointmentIdOrderByCreatedAtDesc(APPOINTMENT_ID) } returns null
      every { appointmentEventEntityFactory.buildUpdatedEvent(any(), any(), any()) } returns AppointmentEventEntity.fromUpdateRequest(updateRequest)
      every { appointmentEventEntityRepository.save(any()) } returnsArgument 0

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
      every { appointmentEventEntityRepository.findTopByDeliusAppointmentIdOrderByCreatedAtDesc(APPOINTMENT_ID) } returns null
      every { appointmentEventEntityFactory.buildUpdatedEvent(any(), any(), any()) } returns AppointmentEventEntity.fromUpdateRequest(updateRequest)
      every { appointmentEventEntityRepository.save(any()) } returnsArgument 0

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

    @Test
    fun `if there's an existing entry and form data key is specified, remove the form data`() {
      val formKey = FormKeyDto(
        id = "formKeyId",
        type = "formKeyType",
      )

      val updateRequest = updateRequest.copy(formKeyToDelete = formKey)

      every { appointmentEventEntityRepository.save(any()) } returnsArgument 0
      every { appointmentEventEntityFactory.buildUpdatedEvent(any(), any(), any()) } returns AppointmentEventEntity.fromUpdateRequest(updateRequest)

      service.updateAppointmentOutcome(
        projectCode = PROJECT_CODE,
        update = updateRequest,
        trigger = TRIGGER,
      )

      verify { formService.deleteIfExists(formKey) }
    }
  }

  fun AppointmentEventEntity.Companion.fromUpdateRequest(updateRequest: UpdateAppointmentOutcomeDto) = AppointmentEventEntity.valid()
    .copy(
      eventType = AppointmentEventType.UPDATE,
      deliusAppointmentId = updateRequest.deliusId,
      priorDeliusVersion = updateRequest.deliusVersionToUpdate,
    )
}
