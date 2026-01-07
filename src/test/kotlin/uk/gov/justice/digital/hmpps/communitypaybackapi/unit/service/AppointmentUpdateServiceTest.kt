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
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdditionalInformationType
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentOutcomeEntityFactory
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentOutcomeValidationService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentRetrievalService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentUpdateService
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
  lateinit var appointmentOutcomeEntityRepository: AppointmentOutcomeEntityRepository

  @RelaxedMockK
  lateinit var communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient

  @RelaxedMockK
  lateinit var formService: FormService

  @RelaxedMockK
  private lateinit var appointmentOutcomeValidationService: AppointmentOutcomeValidationService

  @RelaxedMockK
  private lateinit var appointmentOutcomeEntityFactory: AppointmentOutcomeEntityFactory

  @RelaxedMockK
  private lateinit var domainEventService: DomainEventService

  @InjectMockKs
  private lateinit var service: AppointmentUpdateService

  private companion object {
    const val PROJECT_CODE = "PROJ123"
    const val APPOINTMENT_ID = 101L
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
        )
      }.isSameAs(upstreamException)
    }

    @Test
    fun `if appointment not found on update, throw not found exception`() {
      every { appointmentRetrievalService.getAppointment(PROJECT_CODE, APPOINTMENT_ID) } returns existingAppointment
      every { appointmentOutcomeEntityRepository.findTopByAppointmentDeliusIdOrderByCreatedAtDesc(APPOINTMENT_ID) } returns null
      every { appointmentOutcomeEntityFactory.toEntity(any(), any()) } returns AppointmentOutcomeEntity.fromUpdateRequest(updateRequest)
      every { appointmentOutcomeEntityRepository.save(any()) } returnsArgument 0

      every {
        communityPaybackAndDeliusClient.updateAppointment(any(), any(), any())
      } throws WebClientResponseExceptionFactory.notFound()

      assertThatThrownBy {
        service.updateAppointmentOutcome(
          projectCode = PROJECT_CODE,
          update = updateRequest,
        )
      }.isInstanceOf(NotFoundException::class.java).hasMessage("Appointment not found for ID '$APPOINTMENT_ID'")
    }

    @Test
    fun `if there's no existing entries for the delius appointment ids, persist new entry, raise domain event and invoke update endpoint`() {
      every { appointmentRetrievalService.getAppointment(PROJECT_CODE, APPOINTMENT_ID) } returns existingAppointment
      every { appointmentOutcomeEntityRepository.findTopByAppointmentDeliusIdOrderByCreatedAtDesc(APPOINTMENT_ID) } returns null

      val entityReturnedByFactory = AppointmentOutcomeEntity.fromUpdateRequest(updateRequest)
      every { appointmentOutcomeEntityFactory.toEntity(updateRequest, existingAppointment) } returns entityReturnedByFactory
      every { appointmentOutcomeEntityRepository.save(any()) } returnsArgument 0

      service.updateAppointmentOutcome(
        projectCode = PROJECT_CODE,
        update = updateRequest,
      )

      verify {
        appointmentOutcomeEntityRepository.save(entityReturnedByFactory)
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
      val existingIdenticalEntity = AppointmentOutcomeEntity.fromUpdateRequest(updateRequest)

      every { appointmentRetrievalService.getAppointment(PROJECT_CODE, APPOINTMENT_ID) } returns existingAppointment
      every { appointmentOutcomeEntityRepository.findTopByAppointmentDeliusIdOrderByCreatedAtDesc(APPOINTMENT_ID) } returns existingIdenticalEntity
      every { appointmentOutcomeEntityFactory.toEntity(updateRequest, existingAppointment) } returns existingIdenticalEntity

      service.updateAppointmentOutcome(
        projectCode = PROJECT_CODE,
        update = updateRequest,
      )

      verify(exactly = 0) {
        appointmentOutcomeEntityRepository.save(any())
        communityPaybackAndDeliusClient.updateAppointment(any(), any(), any())
      }
    }

    @Test
    fun `if there's an existing entry for the delius appointment id but it's not logically identical, persist new entry send an update`() {
      val existingOutcomeEntity = AppointmentOutcomeEntity.fromUpdateRequest(updateRequest)

      every { appointmentRetrievalService.getAppointment(PROJECT_CODE, APPOINTMENT_ID) } returns existingAppointment
      every { appointmentOutcomeEntityRepository.findTopByAppointmentDeliusIdOrderByCreatedAtDesc(APPOINTMENT_ID) } returns existingOutcomeEntity
      every { appointmentOutcomeEntityFactory.toEntity(updateRequest, existingAppointment) } returns AppointmentOutcomeEntity.fromUpdateRequest(updateRequest).copy(appointmentDeliusId = APPOINTMENT_ID)
      every { appointmentOutcomeEntityRepository.save(any()) } returnsArgument 0

      service.updateAppointmentOutcome(
        projectCode = PROJECT_CODE,
        update = updateRequest,
      )

      verify { appointmentOutcomeEntityRepository.save(any()) }
      verify { communityPaybackAndDeliusClient.updateAppointment(PROJECT_CODE, APPOINTMENT_ID, any()) }
    }

    @Test
    fun `if appointment has newer version on update, throw conflict exception`() {
      every { appointmentRetrievalService.getAppointment(PROJECT_CODE, APPOINTMENT_ID) } returns existingAppointment
      every { appointmentOutcomeEntityRepository.findTopByAppointmentDeliusIdOrderByCreatedAtDesc(APPOINTMENT_ID) } returns null
      every { appointmentOutcomeEntityFactory.toEntity(any(), any()) } returns AppointmentOutcomeEntity.fromUpdateRequest(updateRequest)
      every { appointmentOutcomeEntityRepository.save(any()) } returnsArgument 0

      every {
        communityPaybackAndDeliusClient.updateAppointment(any(), any(), any())
      } throws WebClientResponseExceptionFactory.conflict()

      assertThatThrownBy {
        service.updateAppointmentOutcome(
          projectCode = PROJECT_CODE,
          update = updateRequest,
        )
      }.isInstanceOf(ConflictException::class.java).hasMessage("A newer version of the appointment exists. Stale version is '${updateRequest.deliusVersionToUpdate}'")
    }

    @Test
    fun `if bad request returned throw internal server error`() {
      every { appointmentOutcomeEntityRepository.findTopByAppointmentDeliusIdOrderByCreatedAtDesc(APPOINTMENT_ID) } returns null
      every { appointmentOutcomeEntityFactory.toEntity(any(), any()) } returns AppointmentOutcomeEntity.fromUpdateRequest(updateRequest)
      every { appointmentOutcomeEntityRepository.save(any()) } returnsArgument 0

      every {
        communityPaybackAndDeliusClient.updateAppointment(any(), any(), any())
      } throws WebClientResponseExceptionFactory.badRequest("didn't look good")

      assertThatThrownBy {
        service.updateAppointmentOutcome(
          projectCode = PROJECT_CODE,
          update = updateRequest,
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

      every { appointmentOutcomeEntityRepository.save(any()) } returnsArgument 0

      service.updateAppointmentOutcome(
        projectCode = PROJECT_CODE,
        update = updateRequest,
      )

      verify { formService.deleteIfExists(formKey) }
    }
  }

  fun AppointmentOutcomeEntity.Companion.fromUpdateRequest(updateRequest: UpdateAppointmentOutcomeDto) = AppointmentOutcomeEntity.valid()
    .copy(appointmentDeliusId = updateRequest.deliusId, deliusVersionToUpdate = updateRequest.deliusVersionToUpdate)
}
