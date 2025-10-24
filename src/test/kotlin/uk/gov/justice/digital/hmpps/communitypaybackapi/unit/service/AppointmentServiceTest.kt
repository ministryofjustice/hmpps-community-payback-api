package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ConflictException
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.FormKeyDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentOutcomeEntityFactory
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentOutcomeValidationService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.FormService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.AppointmentMappers
import uk.gov.justice.digital.hmpps.communitypaybackapi.unit.util.WebClientResponseExceptionFactory
import java.util.UUID

@ExtendWith(MockKExtension::class)
class AppointmentServiceTest {

  @MockK(relaxed = true)
  lateinit var appointmentOutcomeEntityRepository: AppointmentOutcomeEntityRepository

  @MockK(relaxed = true)
  lateinit var communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient

  @MockK(relaxed = true)
  lateinit var appointmentMappers: AppointmentMappers

  @MockK(relaxed = true)
  lateinit var formService: FormService

  @SuppressWarnings("UnusedPrivateProperty")
  @MockK(relaxed = true)
  private lateinit var appointmentOutcomeValidationService: AppointmentOutcomeValidationService

  @MockK(relaxed = true)
  private lateinit var appointmentOutcomeEntityFactory: AppointmentOutcomeEntityFactory

  @InjectMockKs
  private lateinit var service: AppointmentService

  @Nested
  inner class GetAppointment {

    @Test
    fun `if appointment not found, throw not found exception`() {
      every { communityPaybackAndDeliusClient.getProjectAppointment(101L) } throws WebClientResponseExceptionFactory.notFound()

      assertThatThrownBy {
        service.getAppointment(101L)
      }.isInstanceOf(NotFoundException::class.java).hasMessage("Appointment not found for ID '101'")
    }

    @Test
    fun `appointment found`() {
      val appointment = ProjectAppointment.valid()
      every { communityPaybackAndDeliusClient.getProjectAppointment(101L) } returns appointment

      val appointmentDto = AppointmentDto.valid()
      every { appointmentMappers.toDto(appointment) } returns appointmentDto

      val result = service.getAppointment(101L)

      assertThat(result).isSameAs(appointmentDto)
    }
  }

  @Nested
  inner class UpdateAppointmentOutcome {

    @Test
    fun `if appointment not found, throw not found exception`() {
      every { appointmentOutcomeEntityRepository.findTopByAppointmentDeliusIdOrderByCreatedAtDesc(1L) } returns null
      every { appointmentOutcomeEntityFactory.toEntity(any(), any()) } returns AppointmentOutcomeEntity.valid()
      every { appointmentOutcomeEntityRepository.save(any()) } returnsArgument 0

      every {
        communityPaybackAndDeliusClient.updateAppointment(any(), any())
      } throws WebClientResponseExceptionFactory.notFound()

      assertThatThrownBy {
        service.updateAppointmentOutcome(
          deliusId = 101L,
          outcome = UpdateAppointmentOutcomeDto.valid(),
        )
      }.isInstanceOf(NotFoundException::class.java).hasMessage("Appointment not found for ID '101'")
    }

    @Test
    fun `if appointment has newer version, throw conflict exception`() {
      every { appointmentOutcomeEntityRepository.findTopByAppointmentDeliusIdOrderByCreatedAtDesc(1L) } returns null
      every { appointmentOutcomeEntityFactory.toEntity(any(), any()) } returns AppointmentOutcomeEntity.valid()
      every { appointmentOutcomeEntityRepository.save(any()) } returnsArgument 0

      every {
        communityPaybackAndDeliusClient.updateAppointment(any(), any())
      } throws WebClientResponseExceptionFactory.conflict()

      val version = UUID.randomUUID()

      assertThatThrownBy {
        service.updateAppointmentOutcome(
          deliusId = 101L,
          outcome = UpdateAppointmentOutcomeDto.valid().copy(deliusVersionToUpdate = version),
        )
      }.isInstanceOf(ConflictException::class.java).hasMessage("A newer version of the appointment exists. Stale version is '$version'")
    }

    @Test
    fun `if there's no existing entries for the delius appointment ids, persist new entry and invoke update endpoint`() {
      val updateOutcomeDto = UpdateAppointmentOutcomeDto.valid()

      every { appointmentOutcomeEntityRepository.findTopByAppointmentDeliusIdOrderByCreatedAtDesc(1L) } returns null

      val entityReturnedByFactory = AppointmentOutcomeEntity.valid()
      every {
        appointmentOutcomeEntityFactory.toEntity(101L, updateOutcomeDto)
      } returns entityReturnedByFactory

      val entityCaptor = mutableListOf<AppointmentOutcomeEntity>()
      every { appointmentOutcomeEntityRepository.save(capture(entityCaptor)) } returnsArgument 0

      service.updateAppointmentOutcome(
        deliusId = 101L,
        outcome = updateOutcomeDto,
      )

      assertThat(entityCaptor).hasSize(1)
      assertThat(entityCaptor[0]).isSameAs(entityReturnedByFactory)

      verify {
        communityPaybackAndDeliusClient.updateAppointment(101L, any())
      }
    }

    @Test
    fun `if there's an existing entry and form data key is specified, remove the form data`() {
      every {
        communityPaybackAndDeliusClient.getProjectAppointment(101L)
      } returns ProjectAppointment.valid().copy(case = CaseSummary.valid().copy(crn = "CRN1"))

      every { appointmentOutcomeEntityRepository.save(any()) } returnsArgument 0

      service.updateAppointmentOutcome(
        deliusId = 101L,
        outcome = UpdateAppointmentOutcomeDto.valid().copy(
          formKeyToDelete = FormKeyDto(
            id = "formKeyId",
            type = "formKeyType",
          ),
        ),
      )
    }

    @Test
    fun `if there's an existing entry for the delius appointment id and it's logically identical, do not persist a new entry`() {
      val updateAppointmentDto = UpdateAppointmentOutcomeDto.valid()

      val existingIdenticalEntity = AppointmentOutcomeEntity.valid()
      every {
        appointmentOutcomeEntityRepository.findTopByAppointmentDeliusIdOrderByCreatedAtDesc(1L)
      } returns existingIdenticalEntity

      every { appointmentOutcomeEntityFactory.toEntity(1L, updateAppointmentDto) } returns existingIdenticalEntity

      service.updateAppointmentOutcome(
        deliusId = 1L,
        outcome = updateAppointmentDto,
      )

      verify(exactly = 0) { appointmentOutcomeEntityRepository.save(any()) }
    }

    @Test
    fun `if there's an existing entry for the delius appointment id but it's not logically identical, persist new entry send an update`() {
      val updateAppointmentDto = UpdateAppointmentOutcomeDto.valid()

      val existingAlmostIdenticalEntity = AppointmentOutcomeEntity.valid().copy(notes = "some different notes")
      every {
        appointmentOutcomeEntityRepository.findTopByAppointmentDeliusIdOrderByCreatedAtDesc(1L)
      } returns existingAlmostIdenticalEntity

      every {
        appointmentOutcomeEntityRepository.save(any())
      } returnsArgument 0

      service.updateAppointmentOutcome(
        deliusId = 1L,
        outcome = updateAppointmentDto,
      )

      verify { appointmentOutcomeEntityRepository.save(any()) }
      verify { communityPaybackAndDeliusClient.updateAppointment(1L, any()) }
    }
  }
}
