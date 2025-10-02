package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.project.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAllocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAllocations
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAppointments
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.OffenderInfoResult
import uk.gov.justice.digital.hmpps.communitypaybackapi.project.dto.ProjectAllocationDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.project.service.toDto
import java.time.LocalDate
import java.time.LocalTime

class ProjectMappersTest {

  @Nested
  inner class ProjectAllocationsMapper {

    @Test
    fun `should map ProjectAllocations to DTO correctly`() {
      val projectAllocations = ProjectAllocations(
        listOf(
          ProjectAllocation(
            id = 1L,
            projectId = 101L,
            projectName = "Community Garden",
            date = LocalDate.of(2025, 9, 1),
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(17, 0),
            projectCode = "cg",
            numberOfOffendersAllocated = 0,
            numberOfOffendersWithOutcomes = 1,
            numberOfOffendersWithEA = 2,
          ),
          ProjectAllocation(
            id = 2L,
            projectId = 102L,
            projectName = "Park Cleanup",
            date = LocalDate.of(2025, 9, 8),
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(16, 0),
            projectCode = "pc",
            numberOfOffendersAllocated = 3,
            numberOfOffendersWithOutcomes = 4,
            numberOfOffendersWithEA = 5,
          ),
        ),
      )

      val projectAllocationsDto = projectAllocations.toDto()

      assertThat(projectAllocationsDto.allocations).hasSize(2)

      assertThat(projectAllocationsDto.allocations[0].id).isEqualTo(1L)
      assertThat(projectAllocationsDto.allocations[0].projectId).isEqualTo(101L)
      assertThat(projectAllocationsDto.allocations[0].projectName).isEqualTo("Community Garden")
      assertThat(projectAllocationsDto.allocations[0].date).isEqualTo(LocalDate.of(2025, 9, 1))
      assertThat(projectAllocationsDto.allocations[0].startTime).isEqualTo(LocalTime.of(9, 0))
      assertThat(projectAllocationsDto.allocations[0].endTime).isEqualTo(LocalTime.of(17, 0))
      assertThat(projectAllocationsDto.allocations[0].projectCode).isEqualTo("cg")
      assertThat(projectAllocationsDto.allocations[0].numberOfOffendersAllocated).isEqualTo(0)
      assertThat(projectAllocationsDto.allocations[0].numberOfOffendersWithOutcomes).isEqualTo(1)
      assertThat(projectAllocationsDto.allocations[0].numberOfOffendersWithEA).isEqualTo(2)

      assertThat(projectAllocationsDto.allocations[1].id).isEqualTo(2L)
      assertThat(projectAllocationsDto.allocations[1].projectId).isEqualTo(102L)
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
      val projectAllocation = ProjectAllocation(
        id = 1L,
        projectId = 2L,
        projectName = "Community Garden",
        date = LocalDate.of(2025, 9, 1),
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(17, 0),
        projectCode = "cg",
        numberOfOffendersAllocated = 40,
        numberOfOffendersWithOutcomes = 0,
        numberOfOffendersWithEA = 0,
      )

      assertThat(projectAllocation.toDto()).isEqualTo(
        ProjectAllocationDto(
          id = 1L,
          projectId = 2L,
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
  inner class ProjectAppointmentsMapper {
    @Test
    fun `should map ProjectAppointments to DTO correctly`() {
      val projectAppointments = ProjectAppointments(
        appointments = listOf(
          ProjectAppointment(
            id = 1L,
            projectName = "Community Garden",
            projectCode = "N987654321",
            requirementMinutes = 520,
            completedMinutes = 30,
            crn = "CRN1",
          ),
          ProjectAppointment(
            id = 2L,
            projectName = "Park Cleanup",
            projectCode = "N987654321",
            requirementMinutes = 20,
            completedMinutes = 10,
            crn = "CRN2",
          ),
        ),
      )

      val result = projectAppointments.toDto(
        offenderInfoResults = listOf(
          OffenderInfoResult.Limited("CRN1"),
          OffenderInfoResult.NotFound("CRN2"),
        ),
      )

      assertThat(result.appointments).hasSize(2)

      assertThat(result.appointments[0].id).isEqualTo(1L)
      assertThat(result.appointments[0].projectName).isEqualTo("Community Garden")
      assertThat(result.appointments[0].requirementMinutes).isEqualTo(520)
      assertThat(result.appointments[0].completedMinutes).isEqualTo(30)
      assertThat(result.appointments[0].offender).isNotNull

      assertThat(result.appointments[1].id).isEqualTo(2L)
      assertThat(result.appointments[1].projectName).isEqualTo("Park Cleanup")
      assertThat(result.appointments[1].requirementMinutes).isEqualTo(20)
      assertThat(result.appointments[1].completedMinutes).isEqualTo(10)
      assertThat(result.appointments[1].offender).isNotNull
    }
  }

  @Nested
  inner class ProjectAppointmentMapper {
    @Test
    fun `should map ProjectAppointment to DTO correctly`() {
      val projectAppointment = ProjectAppointment(
        id = 1L,
        projectName = "Community Garden",
        projectCode = "N987654321",
        requirementMinutes = 520,
        completedMinutes = 30,
        crn = "CRN1",
      )

      val result = projectAppointment.toDto(OffenderInfoResult.Limited("CRN1"))

      assertThat(result.id).isEqualTo(1L)
      assertThat(result.projectName).isEqualTo("Community Garden")
      assertThat(result.requirementMinutes).isEqualTo(520)
      assertThat(result.completedMinutes).isEqualTo(30)
      assertThat(result.offender).isNotNull
    }
  }
}
