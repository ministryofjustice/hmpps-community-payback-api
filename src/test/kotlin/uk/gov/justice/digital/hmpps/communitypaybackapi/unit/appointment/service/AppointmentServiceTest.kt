package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.appointment.service

import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.AppointmentBehaviourDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.AppointmentWorkQualityDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.AttendanceDataDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.EnforcementDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.UpdateAppointmentOutcomesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.UpsertAppointmentDraftDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.AppointmentDraftEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.AppointmentDraftEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.AppointmentOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.AppointmentOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.Behaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.WorkQuality
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.service.AppointmentService
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.dto.BadRequestException
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.dto.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.dto.OffenderDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.AdditionalInformationType
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.DomainEventService
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.DomainEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.OffenderInfoResult
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.OffenderService
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.PersonReferenceType
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.entity.ProjectTypeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.unit.util.WebClientResponseExceptionFactory
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
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

  @InjectMockKs
  private lateinit var service: AppointmentService

  @MockK(relaxed = true)
  lateinit var appointmentDraftEntityRepository: AppointmentDraftEntityRepository

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
      every {
        communityPaybackAndDeliusClient.getProjectAppointment(101L)
      } returns ProjectAppointment.valid().copy(id = 101L, crn = "CRN123")

      every { offenderService.getOffenderInfo("CRN123") } returns OffenderInfoResult.Full.valid(crn = "CRN123")

      val result = service.getAppointment(101L)

      assertThat(result.id).isEqualTo(101L)
      assertThat(result.offender.crn).isEqualTo("CRN123")
      assertThat(result.offender).isInstanceOf(OffenderDto.OffenderFullDto::class.java)
    }
  }

  @Nested
  inner class UpdateAppointmentsOutcome {

    @Test
    fun `if appointment not found, throw bad request exception`() {
      every { communityPaybackAndDeliusClient.getProjectAppointment(101L) } throws WebClientResponseExceptionFactory.notFound()

      assertThatThrownBy {
        service.updateAppointmentsOutcome(UpdateAppointmentOutcomesDto.valid(101L))
      }.isInstanceOf(BadRequestException::class.java).hasMessage("Appointment not found for ID '101'")
    }

    @Test
    fun `if there's no existing entries for the delius appointment ids, persist new entries and raise domain events`() {
      every { communityPaybackAndDeliusClient.getProjectAppointment(1L) } returns ProjectAppointment.valid().copy(crn = "CRN1")
      every { communityPaybackAndDeliusClient.getProjectAppointment(2L) } returns ProjectAppointment.valid().copy(crn = "CRN2")

      every { appointmentOutcomeEntityRepository.findTopByAppointmentDeliusIdOrderByUpdatedAtDesc(1L) } returns null
      every { appointmentOutcomeEntityRepository.findTopByAppointmentDeliusIdOrderByUpdatedAtDesc(2L) } returns null

      val entityCaptor = mutableListOf<AppointmentOutcomeEntity>()
      every { appointmentOutcomeEntityRepository.save(capture(entityCaptor)) } returnsArgument 0

      service.updateAppointmentsOutcome(
        UpdateAppointmentOutcomesDto(
          ids = listOf(1L, 2L),
          outcomeData = UpdateAppointmentOutcomeDto(
            startTime = LocalTime.of(10, 1, 2),
            endTime = LocalTime.of(16, 3, 4),
            contactOutcomeId = UUID.fromString("4306c7ca-b717-4995-9eea-91e41d95d44a"),
            supervisorOfficerCode = "N45",
            notes = "some notes",
            attendanceData = AttendanceDataDto(
              hiVisWorn = false,
              workedIntensively = true,
              penaltyMinutes = 300L,
              workQuality = AppointmentWorkQualityDto.SATISFACTORY,
              behaviour = AppointmentBehaviourDto.UNSATISFACTORY,
            ),
            enforcementData = EnforcementDto(
              enforcementActionId = UUID.fromString("52bffba3-2366-4941-aff5-9418b4fbca7e"),
              respondBy = LocalDate.of(2026, 8, 10),
            ),
          ),
        ),
      )

      assertThat(entityCaptor).hasSize(2)

      val firstEntity = entityCaptor[0]

      assertThat(firstEntity.id).isNotNull
      assertThat(firstEntity.appointmentDeliusId).isEqualTo(1L)
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
          additionalInformation = mapOf(AdditionalInformationType.APPOINTMENT_ID to 1L),
          personReferences = mapOf(PersonReferenceType.CRN to "CRN1"),
        )
      }

      verify {
        domainEventService.publish(
          id = entityCaptor[1].id,
          type = DomainEventType.APPOINTMENT_OUTCOME,
          additionalInformation = mapOf(AdditionalInformationType.APPOINTMENT_ID to 2L),
          personReferences = mapOf(PersonReferenceType.CRN to "CRN2"),
        )
      }
    }

    @Test
    fun `if there's an existing entry for the delius appointment id and it's logically identical, do not persist a new entry`() {
      val updateAppointmentDto = UpdateAppointmentOutcomesDto.valid(1L)

      val existingIdenticalEntity = service.toEntity(1L, updateAppointmentDto.outcomeData)
      every {
        appointmentOutcomeEntityRepository.findTopByAppointmentDeliusIdOrderByUpdatedAtDesc(1L)
      } returns existingIdenticalEntity

      service.updateAppointmentsOutcome(updateAppointmentDto)

      verify(exactly = 0) { appointmentOutcomeEntityRepository.save(any()) }
    }

    @Test
    fun `if there's an existing entry for the delius appointment id but it's not logically identical, persist new entry and raise domain event`() {
      val updateAppointmentDto = UpdateAppointmentOutcomesDto.valid(1L)

      val existingAlmostIdenticalEntity = service.toEntity(1L, updateAppointmentDto.outcomeData)
        .copy(notes = "some different notes")
      every {
        appointmentOutcomeEntityRepository.findTopByAppointmentDeliusIdOrderByUpdatedAtDesc(1L)
      } returns existingAlmostIdenticalEntity

      every {
        appointmentOutcomeEntityRepository.save(any())
      } returnsArgument 0

      every { domainEventService.publish(any(), any(), any()) } just Runs

      service.updateAppointmentsOutcome(updateAppointmentDto)

      verify { appointmentOutcomeEntityRepository.save(any()) }
      verify { domainEventService.publish(any(), DomainEventType.APPOINTMENT_OUTCOME, any(), any()) }
    }
  }

  @Nested
  inner class UpsertAppointmentDraft {

    private fun makeValidRequest(projectTypeId: UUID) = UpsertAppointmentDraftDto(
      crn = "X12345",
      projectName = "Some Project",
      projectCode = "SP01",
      projectTypeId = projectTypeId,
      supervisingTeamCode = "TEAM1",
      appointmentDate = LocalDate.of(2025, 10, 10),
      startTime = LocalTime.of(9, 0),
      endTime = LocalTime.of(14, 0),
      attendanceData = AttendanceDataDto(
        hiVisWorn = true,
        workedIntensively = false,
        penaltyMinutes = 90,
        workQuality = AppointmentWorkQualityDto.GOOD,
        behaviour = AppointmentBehaviourDto.POOR,
        supervisorOfficerCode = "SUP01",
        contactOutcomeId = UUID.fromString("4306c7ca-b717-4995-9eea-91e41d95d44a"),
      ),
      enforcementData = EnforcementDto(
        enforcementActionId = UUID.fromString("52bffba3-2366-4941-aff5-9418b4fbca7e"),
        respondBy = LocalDate.of(2025, 10, 20),
      ),
      notes = "some notes",
      deliusLastUpdatedAt = OffsetDateTime.parse("2025-10-10T10:10:10Z"),
    )

    @Nested
    inner class CreateNewDraft {

      @Test
      fun `when no existing draft for delius id then create new`() {
        val deliusId = 999L
        val projectType = ProjectTypeEntity.valid()

        every { appointmentDraftEntityRepository.findByAppointmentDeliusId(deliusId) } returns null

        val savedSlot = slot<AppointmentDraftEntity>()
        every { appointmentDraftEntityRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        val result = service.upsertAppointmentDraft(deliusId, makeValidRequest(projectType.id))

        val saved = savedSlot.captured
        assertThat(saved.id).isNotNull
        assertThat(saved.appointmentDeliusId).isEqualTo(deliusId)
        assertThat(saved.crn).isEqualTo("X12345")
        assertThat(saved.projectName).isEqualTo("Some Project")
        assertThat(saved.projectCode).isEqualTo("SP01")
        assertThat(saved.projectTypeId).isEqualTo(projectType.id)
        assertThat(saved.supervisingTeamCode).isEqualTo("TEAM1")
        assertThat(saved.appointmentDate).isEqualTo(LocalDate.of(2025, 10, 10))
        assertThat(saved.startTime).isEqualTo(LocalTime.of(9, 0))
        assertThat(saved.endTime).isEqualTo(LocalTime.of(14, 0))
        assertThat(saved.hiVisWorn).isTrue
        assertThat(saved.workedIntensively).isFalse
        assertThat(saved.penaltyTimeMinutes).isEqualTo(90)
        assertThat(saved.workQuality).isEqualTo(WorkQuality.GOOD)
        assertThat(saved.behaviour).isEqualTo(Behaviour.POOR)
        assertThat(saved.supervisorOfficerCode).isEqualTo("SUP01")
        assertThat(saved.contactOutcomeId).isEqualTo(UUID.fromString("4306c7ca-b717-4995-9eea-91e41d95d44a"))
        assertThat(saved.enforcementActionId).isEqualTo(UUID.fromString("52bffba3-2366-4941-aff5-9418b4fbca7e"))
        assertThat(saved.respondBy).isEqualTo(LocalDate.of(2025, 10, 20))
        assertThat(saved.notes).isEqualTo("some notes")
        assertThat(saved.deliusLastUpdatedAt).isEqualTo(OffsetDateTime.parse("2025-10-10T10:10:10Z"))

        assertThat(result.appointmentDeliusId).isEqualTo(deliusId)
        assertThat(result.crn).isEqualTo("X12345")
        assertThat(result.attendanceData!!.penaltyMinutes).isEqualTo(90)
        assertThat(result.enforcementData!!.respondBy).isEqualTo(LocalDate.of(2025, 10, 20))

        verify { appointmentDraftEntityRepository.save(any()) }
      }
    }

    @Nested
    inner class UpdateExistingDraft {

      @Test
      fun `when existing draft for delius id then update it`() {
        val deliusId = 1000L
        val projectType = ProjectTypeEntity.valid()

        val existing = AppointmentDraftEntity(
          id = UUID.randomUUID(),
          appointmentDeliusId = deliusId,
          crn = "X54321",
          projectName = "Another Project",
          projectCode = "ANP",
          projectTypeId = projectType.id,
          projectTypeEntity = null,
          supervisingTeamCode = "SP99",
          appointmentDate = LocalDate.of(2025, 1, 1),
          startTime = LocalTime.of(8, 0),
          endTime = LocalTime.of(9, 0),
          hiVisWorn = null,
          workedIntensively = null,
          penaltyTimeMinutes = null,
          workQuality = null,
          behaviour = null,
          supervisorOfficerCode = null,
          contactOutcomeId = null,
          contactOutcomeEntity = null,
          enforcementActionId = null,
          enforcementActionEntity = null,
          respondBy = null,
          notes = null,
          deliusLastUpdatedAt = OffsetDateTime.parse("2025-01-01T00:00:00Z"),
          createdAt = OffsetDateTime.parse("2025-01-01T00:00:01Z"),
          updatedAt = OffsetDateTime.parse("2025-01-01T00:00:02Z"),
        )

        every { appointmentDraftEntityRepository.findByAppointmentDeliusId(deliusId) } returns existing

        val savedSlot = slot<AppointmentDraftEntity>()
        every { appointmentDraftEntityRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        val req = makeValidRequest(projectType.id)
        val result = service.upsertAppointmentDraft(deliusId, req)

        val saved = savedSlot.captured
        assertThat(saved.id).isEqualTo(existing.id)
        assertThat(saved.appointmentDeliusId).isEqualTo(deliusId)
        assertThat(saved.crn).isEqualTo("X12345")
        assertThat(saved.projectName).isEqualTo("Some Project")
        assertThat(saved.projectCode).isEqualTo("SP01")
        assertThat(saved.projectTypeId).isEqualTo(projectType.id)
        assertThat(saved.supervisingTeamCode).isEqualTo("TEAM1")
        assertThat(saved.appointmentDate).isEqualTo(LocalDate.of(2025, 10, 10))
        assertThat(saved.startTime).isEqualTo(LocalTime.of(9, 0))
        assertThat(saved.endTime).isEqualTo(LocalTime.of(14, 0))
        assertThat(saved.hiVisWorn).isTrue
        assertThat(saved.workedIntensively).isFalse
        assertThat(saved.penaltyTimeMinutes).isEqualTo(90)
        assertThat(saved.workQuality).isEqualTo(WorkQuality.GOOD)
        assertThat(saved.behaviour).isEqualTo(Behaviour.POOR)
        assertThat(saved.supervisorOfficerCode).isEqualTo("SUP01")
        assertThat(saved.contactOutcomeId).isEqualTo(UUID.fromString("4306c7ca-b717-4995-9eea-91e41d95d44a"))
        assertThat(saved.enforcementActionId).isEqualTo(UUID.fromString("52bffba3-2366-4941-aff5-9418b4fbca7e"))
        assertThat(saved.respondBy).isEqualTo(LocalDate.of(2025, 10, 20))
        assertThat(saved.notes).isEqualTo("some notes")
        assertThat(saved.deliusLastUpdatedAt).isEqualTo(OffsetDateTime.parse("2025-10-10T10:10:10Z"))

        assertThat(result.attendanceData!!.penaltyMinutes).isEqualTo(90)
        assertThat(result.enforcementData!!.enforcementActionId).isEqualTo(UUID.fromString("52bffba3-2366-4941-aff5-9418b4fbca7e"))

        verify { appointmentDraftEntityRepository.save(any()) }
      }
    }
  }
}
