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
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.FormKeyDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.OffenderDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentOutcomeEntityFactory
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentOutcomeValidationService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.FormService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.OffenderInfoResult
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.OffenderService
import uk.gov.justice.digital.hmpps.communitypaybackapi.unit.util.WebClientResponseExceptionFactory

@ExtendWith(MockKExtension::class)
class AppointmentServiceTest {

  @MockK(relaxed = true)
  lateinit var appointmentOutcomeEntityRepository: AppointmentOutcomeEntityRepository

  @MockK(relaxed = true)
  lateinit var communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient

  @MockK(relaxed = true)
  lateinit var offenderService: OffenderService

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
      val caseSummary = CaseSummary.valid().copy(crn = "CRN123")

      every {
        communityPaybackAndDeliusClient.getProjectAppointment(101L)
      } returns ProjectAppointment.valid().copy(id = 101L, case = caseSummary)

      every { offenderService.toOffenderInfo(caseSummary) } returns OffenderInfoResult.Full.valid(crn = "CRN123")

      val result = service.getAppointment(101L)

      assertThat(result.id).isEqualTo(101L)
      assertThat(result.offender.crn).isEqualTo("CRN123")
      assertThat(result.offender).isInstanceOf(OffenderDto.OffenderFullDto::class.java)
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
