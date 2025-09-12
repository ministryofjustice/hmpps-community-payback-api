package uk.gov.justice.digital.hmpps.communitypaybackapi.project.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAllocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAllocations
import uk.gov.justice.digital.hmpps.communitypaybackapi.project.controller.ProjectAllocationDto
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
            projectName = "Community Garden",
            teamId = 1L,
            startDate = LocalDate.of(2025, 9, 1),
            endDate = LocalDate.of(2025, 9, 7),
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(17, 0),
            projectCode = "cg",
            allocated = 0,
            outcomes = 1,
            enforcements = 2,
          ),
          ProjectAllocation(
            id = 2L,
            projectName = "Park Cleanup",
            teamId = 1L,
            startDate = LocalDate.of(2025, 9, 8),
            endDate = LocalDate.of(2025, 9, 14),
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(16, 0),
            projectCode = "pc",
            allocated = 3,
            outcomes = 4,
            enforcements = 5,
          ),
        ),
      )

      val projectAllocationsDto = projectAllocations.toDto()

      assertThat(projectAllocationsDto.allocations).hasSize(2)

      assertThat(projectAllocationsDto.allocations[0].id).isEqualTo(1L)
      assertThat(projectAllocationsDto.allocations[0].projectName).isEqualTo("Community Garden")
      assertThat(projectAllocationsDto.allocations[0].teamId).isEqualTo(1L)
      assertThat(projectAllocationsDto.allocations[0].startDate).isEqualTo(LocalDate.of(2025, 9, 1))
      assertThat(projectAllocationsDto.allocations[0].endDate).isEqualTo(LocalDate.of(2025, 9, 7))
      assertThat(projectAllocationsDto.allocations[0].projectCode).isEqualTo("cg")
      assertThat(projectAllocationsDto.allocations[0].allocated).isEqualTo(0)
      assertThat(projectAllocationsDto.allocations[0].outcomes).isEqualTo(1)
      assertThat(projectAllocationsDto.allocations[0].enforcements).isEqualTo(2)

      assertThat(projectAllocationsDto.allocations[1].id).isEqualTo(2L)
      assertThat(projectAllocationsDto.allocations[1].projectName).isEqualTo("Park Cleanup")
      assertThat(projectAllocationsDto.allocations[1].teamId).isEqualTo(1L)
      assertThat(projectAllocationsDto.allocations[1].startDate).isEqualTo(LocalDate.of(2025, 9, 8))
      assertThat(projectAllocationsDto.allocations[1].endDate).isEqualTo(LocalDate.of(2025, 9, 14))
      assertThat(projectAllocationsDto.allocations[1].projectCode).isEqualTo("pc")
      assertThat(projectAllocationsDto.allocations[1].allocated).isEqualTo(3)
      assertThat(projectAllocationsDto.allocations[1].outcomes).isEqualTo(4)
      assertThat(projectAllocationsDto.allocations[1].enforcements).isEqualTo(5)
    }
  }

  @Nested
  inner class ProjectAllocationMapper {
    @Test
    fun `should map ProjectAllocation to DTO correctly`() {
      val projectAllocation = ProjectAllocation(
        id = 1L,
        projectName = "Community Garden",
        teamId = 1L,
        startDate = LocalDate.of(2025, 9, 1),
        endDate = LocalDate.of(2025, 9, 7),
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(17, 0),
        projectCode = "cg",
        allocated = 40,
        outcomes = 0,
        enforcements = 0,
      )

      assertThat(projectAllocation.toDto()).isEqualTo(
        ProjectAllocationDto(
          id = 1L,
          projectName = "Community Garden",
          teamId = 1L,
          startDate = LocalDate.of(2025, 9, 1),
          endDate = LocalDate.of(2025, 9, 7),
          projectCode = "cg",
          allocated = 40,
          outcomes = 0,
          enforcements = 0,
        ),
      )
    }
  }
}
