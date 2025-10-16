package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.project.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.AppointmentBehaviourDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.AppointmentWorkQualityDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.service.toDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAppointmentBehaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAppointmentSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAppointmentWorkQuality
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectSession
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectSessionSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.dto.OffenderDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.OffenderInfoResult
import uk.gov.justice.digital.hmpps.communitypaybackapi.project.dto.SessionSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.project.service.toDto
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class ProjectMappersTest {

  @Nested
  inner class ProjectAllocationsMapper {

    @Test
    fun `should map ProjectAllocations to DTO correctly`() {
      val projectSessions = ProjectSessionSummaries(
        listOf(
          ProjectSummary(
            projectName = "Community Garden",
            date = LocalDate.of(2025, 9, 1),
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(17, 0),
            projectCode = "cg",
            allocatedCount = 0,
            compliedOutcomeCount = 1,
            enforcementActionNeededCount = 2,
          ),
          ProjectSummary(
            projectName = "Park Cleanup",
            date = LocalDate.of(2025, 9, 8),
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(16, 0),
            projectCode = "pc",
            allocatedCount = 3,
            compliedOutcomeCount = 4,
            enforcementActionNeededCount = 5,
          ),
        ),
      )

      val projectAllocationsDto = projectSessions.toDto()

      assertThat(projectAllocationsDto.allocations).hasSize(2)

      assertThat(projectAllocationsDto.allocations[0].projectName).isEqualTo("Community Garden")
      assertThat(projectAllocationsDto.allocations[0].date).isEqualTo(LocalDate.of(2025, 9, 1))
      assertThat(projectAllocationsDto.allocations[0].startTime).isEqualTo(LocalTime.of(9, 0))
      assertThat(projectAllocationsDto.allocations[0].endTime).isEqualTo(LocalTime.of(17, 0))
      assertThat(projectAllocationsDto.allocations[0].projectCode).isEqualTo("cg")
      assertThat(projectAllocationsDto.allocations[0].numberOfOffendersAllocated).isEqualTo(0)
      assertThat(projectAllocationsDto.allocations[0].numberOfOffendersWithOutcomes).isEqualTo(1)
      assertThat(projectAllocationsDto.allocations[0].numberOfOffendersWithEA).isEqualTo(2)

      assertThat(projectAllocationsDto.allocations[1].projectName).isEqualTo("Park Cleanup")
      assertThat(projectAllocationsDto.allocations[1].date).isEqualTo(LocalDate.of(2025, 9, 8))
      assertThat(projectAllocationsDto.allocations[1].startTime).isEqualTo(LocalTime.of(8, 0))
      assertThat(projectAllocationsDto.allocations[1].endTime).isEqualTo(LocalTime.of(16, 0))
      assertThat(projectAllocationsDto.allocations[1].projectCode).isEqualTo("pc")
      assertThat(projectAllocationsDto.allocations[1].numberOfOffendersAllocated).isEqualTo(3)
      assertThat(projectAllocationsDto.allocations[1].numberOfOffendersWithOutcomes).isEqualTo(4)
      assertThat(projectAllocationsDto.allocations[1].numberOfOffendersWithEA).isEqualTo(5)
    }
  }

  @Nested
  inner class ProjectAllocationMapper {
    @Test
    fun `should map ProjectAllocation to DTO correctly`() {
      val projectAllocation = ProjectSummary(
        projectName = "Community Garden",
        date = LocalDate.of(2025, 9, 1),
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(17, 0),
        projectCode = "cg",
        allocatedCount = 40,
        compliedOutcomeCount = 0,
        enforcementActionNeededCount = 0,
      )

      assertThat(projectAllocation.toDto()).isEqualTo(
        SessionSummaryDto(
          id = 0L,
          projectId = 0L,
          projectName = "Community Garden",
          date = LocalDate.of(2025, 9, 1),
          startTime = LocalTime.of(9, 0),
          endTime = LocalTime.of(17, 0),
          projectCode = "cg",
          numberOfOffendersAllocated = 40,
          numberOfOffendersWithOutcomes = 0,
          numberOfOffendersWithEA = 0,
        ),
      )
    }
  }

  @Nested
  inner class ProjectSessionMapper {
    @Test
    fun `should map ProjectSession to DTO correctly`() {
      val projectSession = ProjectSession(
        projectName = "Park Cleanup",
        projectCode = "N987654321",
        projectLocation = "Somwhere Lane, Surrey",
        date = LocalDate.of(2025, 9, 8),
        sessionStartTime = LocalTime.of(8, 0),
        sessionEndTime = LocalTime.of(16, 0),
        appointmentSummaries = listOf(
          ProjectAppointmentSummary(
            appointmentId = 1L,
            requirementMinutes = 520,
            completedMinutes = 30,
            crn = "CRN1",
          ),
          ProjectAppointmentSummary(
            appointmentId = 2L,
            requirementMinutes = 20,
            completedMinutes = 10,
            crn = "CRN2",
          ),
        ),
      )

      val result = projectSession.toDto(
        offenderInfoResults = listOf(
          OffenderInfoResult.Limited("CRN1"),
          OffenderInfoResult.NotFound("CRN2"),
        ),
      )

      assertThat(result.projectName).isEqualTo("Park Cleanup")
      assertThat(result.projectCode).isEqualTo("N987654321")
      assertThat(result.projectLocation).isEqualTo("Somwhere Lane, Surrey")
      assertThat(result.date).isEqualTo(LocalDate.of(2025, 9, 8))
      assertThat(result.startTime).isEqualTo(LocalTime.of(8, 0))
      assertThat(result.endTime).isEqualTo(LocalTime.of(16, 0))
      assertThat(result.appointmentSummaries).hasSize(2)
      assertThat(result.appointmentSummaries[0].id).isEqualTo(1L)
      assertThat(result.appointmentSummaries[0].requirementMinutes).isEqualTo(520)
      assertThat(result.appointmentSummaries[0].completedMinutes).isEqualTo(30)
      assertThat(result.appointmentSummaries[0].offender).isNotNull

      assertThat(result.appointmentSummaries[1].id).isEqualTo(2L)
      assertThat(result.appointmentSummaries[1].requirementMinutes).isEqualTo(20)
      assertThat(result.appointmentSummaries[1].completedMinutes).isEqualTo(10)
      assertThat(result.appointmentSummaries[1].offender).isNotNull
    }
  }

  @Nested
  inner class ProjectAppointmentMapper {
    @Test
    fun `should map ProjectAppointment to DTO correctly`() {
      val id = 101L
      val projectName = "Community Garden Maintenance"
      val projectCode = "CGM101"
      val projectTypeName = "MAINTENANCE"
      val projectTypeCode = "MAINT"
      val crn = "CRN1"
      val contactOutcomeId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
      val enforcementActionId = UUID.fromString("123e4567-e89b-12d3-a456-426614174001")
      val supervisingTeam = "Team Lincoln"
      val supervisingTeamCode = "TL01"
      val providerCode = "PC01"
      val date = LocalDate.of(2025, 9, 1)
      val startTime = LocalTime.of(9, 0)
      val endTime = LocalTime.of(17, 0)
      val penaltyTime = LocalTime.of(0, 0)
      val supervisorCode = "CRN1"
      val respondBy = LocalDate.of(2025, 10, 1)
      val hiVisWorn = true
      val workedIntensively = false
      val workQuality = ProjectAppointmentWorkQuality.SATISFACTORY
      val behaviour = ProjectAppointmentBehaviour.SATISFACTORY
      val notes = "This is a test note"
      val projectAppointment = ProjectAppointment(
        id = id,
        projectName = projectName,
        projectCode = projectCode,
        projectTypeName = projectTypeName,
        projectTypeCode = projectTypeCode,
        crn = crn,
        supervisingTeam = supervisingTeam,
        supervisingTeamCode = supervisingTeamCode,
        providerCode = providerCode,
        date = date,
        startTime = startTime,
        endTime = endTime,
        penaltyTime = penaltyTime,
        supervisorCode = supervisorCode,
        contactOutcomeId = contactOutcomeId,
        enforcementActionId = enforcementActionId,
        respondBy = respondBy,
        hiVisWorn = hiVisWorn,
        workedIntensively = workedIntensively,
        workQuality = workQuality,
        behaviour = behaviour,
        notes = notes,
      )

      val result = projectAppointment.toDto(OffenderInfoResult.Limited("CRN1"))

      assertThat(result.id).isEqualTo(id)
      assertThat(result.projectName).isEqualTo(projectName)
      assertThat(result.projectCode).isEqualTo(projectCode)
      assertThat(result.date).isEqualTo(date)
      assertThat(result.supervisingTeam).isEqualTo(supervisingTeam)
      assertThat(result.supervisingTeamCode).isEqualTo(supervisingTeamCode)
      assertThat(result.providerCode).isEqualTo(providerCode)
      assertThat(result.attendanceData?.supervisorOfficerCode).isEqualTo(supervisorCode)
      assertThat(result.attendanceData?.penaltyTime).isEqualTo(penaltyTime)
      assertThat(result.attendanceData?.behaviour).isEqualTo(AppointmentBehaviourDto.SATISFACTORY)
      assertThat(result.attendanceData?.workQuality).isEqualTo(AppointmentWorkQualityDto.SATISFACTORY)
      assertThat(result.attendanceData?.hiVisWorn).isEqualTo(hiVisWorn)
      assertThat(result.attendanceData?.contactOutcomeId).isEqualTo(contactOutcomeId)
      assertThat(result.enforcementData?.enforcementActionId).isEqualTo(enforcementActionId)
      assertThat(result.enforcementData?.respondBy).isEqualTo(respondBy)
      assertThat(result.notes).isEqualTo(notes)
      assertThat(result.offender.crn).isEqualTo(crn)
      assertThat(result.offender).isInstanceOf(OffenderDto.OffenderLimitedDto::class.java)
    }
  }
}
