package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentBehaviourDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentWorkQualityDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AttendanceDataDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.EnforcementDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.FormKeyDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.OffenderDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.Behaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.WorkQuality
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdditionalInformationType
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentOutcomeValidationService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.DomainEventService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.DomainEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.FormService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.OffenderInfoResult
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.OffenderService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.PersonReferenceType
import uk.gov.justice.digital.hmpps.communitypaybackapi.unit.util.WebClientResponseExceptionFactory
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@ExtendWith(MockKExtension::class)
class AppointmentServiceTest {

  @MockK(relaxed = true)
  lateinit var appointmentOutcomeEntityRepository: AppointmentOutcomeEntityRepository

  @MockK(relaxed = true)
  lateinit var domainEventService: DomainEventService

  @MockK(relaxed = true)
  lateinit var communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient

  @MockK(relaxed = true)
  lateinit var offenderService: OffenderService

  @MockK(relaxed = true)
  lateinit var formService: FormService

  @SuppressWarnings("UnusedPrivateProperty")
  @MockK(relaxed = true)
  private lateinit var appointmentOutcomeValidationService: AppointmentOutcomeValidationService

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
      every { communityPaybackAndDeliusClient.getProjectAppointment(101L) } throws WebClientResponseExceptionFactory.notFound()

      assertThatThrownBy {
        service.updateAppointmentOutcome(
          deliusId = 101L,
          outcome = UpdateAppointmentOutcomeDto.valid(),
        )
      }.isInstanceOf(NotFoundException::class.java).hasMessage("Appointment not found for ID '101'")
    }

    @Test
    fun `if there's no existing entries for the delius appointment ids, persist new entry and raise domain events`() {
      every {
        communityPaybackAndDeliusClient.getProjectAppointment(101L)
      } returns ProjectAppointment.valid().copy(case = CaseSummary.valid().copy(crn = "CRN1"))

      every { appointmentOutcomeEntityRepository.findTopByAppointmentDeliusIdOrderByCreatedAtDesc(1L) } returns null

      val entityCaptor = mutableListOf<AppointmentOutcomeEntity>()
      every { appointmentOutcomeEntityRepository.save(capture(entityCaptor)) } returnsArgument 0

      service.updateAppointmentOutcome(
        deliusId = 101L,
        outcome = UpdateAppointmentOutcomeDto(
          startTime = LocalTime.of(10, 1, 2),
          endTime = LocalTime.of(16, 3, 4),
          contactOutcomeId = UUID.fromString("4306c7ca-b717-4995-9eea-91e41d95d44a"),
          supervisorOfficerCode = "N45",
          notes = "some notes",
          attendanceData = AttendanceDataDto(
            hiVisWorn = false,
            workedIntensively = true,
            penaltyTime = LocalTime.of(5, 0),
            workQuality = AppointmentWorkQualityDto.SATISFACTORY,
            behaviour = AppointmentBehaviourDto.UNSATISFACTORY,
          ),
          enforcementData = EnforcementDto(
            enforcementActionId = UUID.fromString("52bffba3-2366-4941-aff5-9418b4fbca7e"),
            respondBy = LocalDate.of(2026, 8, 10),
          ),
          formKeyToDelete = null,
        ),
      )

      assertThat(entityCaptor).hasSize(1)

      val firstEntity = entityCaptor[0]

      assertThat(firstEntity.id).isNotNull
      assertThat(firstEntity.appointmentDeliusId).isEqualTo(101L)
      assertThat(firstEntity.startTime).isEqualTo(LocalTime.of(10, 1, 2))
      assertThat(firstEntity.endTime).isEqualTo(LocalTime.of(16, 3, 4))
      assertThat(firstEntity.contactOutcomeId).isEqualTo(UUID.fromString("4306c7ca-b717-4995-9eea-91e41d95d44a"))
      assertThat(firstEntity.enforcementActionId).isEqualTo(UUID.fromString("52bffba3-2366-4941-aff5-9418b4fbca7e"))
      assertThat(firstEntity.supervisorOfficerCode).isEqualTo("N45")
      assertThat(firstEntity.notes).isEqualTo("some notes")
      assertThat(firstEntity.hiVisWorn).isEqualTo(false)
      assertThat(firstEntity.workedIntensively).isEqualTo(true)
      assertThat(firstEntity.penaltyMinutes).isEqualTo(300L)
      assertThat(firstEntity.workQuality).isEqualTo(WorkQuality.SATISFACTORY)
      assertThat(firstEntity.behaviour).isEqualTo(Behaviour.UNSATISFACTORY)
      assertThat(firstEntity.respondBy).isEqualTo(LocalDate.of(2026, 8, 10))

      verify {
        domainEventService.publish(
          id = entityCaptor[0].id,
          type = DomainEventType.APPOINTMENT_OUTCOME,
          additionalInformation = mapOf(AdditionalInformationType.APPOINTMENT_ID to 101L),
          personReferences = mapOf(PersonReferenceType.CRN to "CRN1"),
        )
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

      val existingIdenticalEntity = service.toEntity(1L, updateAppointmentDto)
      every {
        appointmentOutcomeEntityRepository.findTopByAppointmentDeliusIdOrderByCreatedAtDesc(1L)
      } returns existingIdenticalEntity

      every { communityPaybackAndDeliusClient.getProjectAppointment(1L) } returns ProjectAppointment.valid().copy(case = CaseSummary.valid().copy(crn = "CRN1"))

      service.updateAppointmentOutcome(
        deliusId = 1L,
        outcome = updateAppointmentDto,
      )

      verify(exactly = 0) { appointmentOutcomeEntityRepository.save(any()) }
    }

    @Test
    fun `if there's an existing entry for the delius appointment id but it's not logically identical, persist new entry and raise domain event`() {
      val updateAppointmentDto = UpdateAppointmentOutcomeDto.valid()

      val existingAlmostIdenticalEntity = service.toEntity(1L, updateAppointmentDto)
        .copy(notes = "some different notes")
      every {
        appointmentOutcomeEntityRepository.findTopByAppointmentDeliusIdOrderByCreatedAtDesc(1L)
      } returns existingAlmostIdenticalEntity

      every {
        appointmentOutcomeEntityRepository.save(any())
      } returnsArgument 0

      every { domainEventService.publish(any(), any(), any()) } just Runs

      every { communityPaybackAndDeliusClient.getProjectAppointment(1L) } returns ProjectAppointment.valid().copy(case = CaseSummary.valid().copy(crn = "CRN1"))

      service.updateAppointmentOutcome(
        deliusId = 1L,
        outcome = updateAppointmentDto,
      )

      verify { appointmentOutcomeEntityRepository.save(any()) }
      verify { domainEventService.publish(any(), DomainEventType.APPOINTMENT_OUTCOME, any(), any()) }
    }
  }
}
