package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.project.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAllocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAllocations
import uk.gov.justice.digital.hmpps.communitypaybackapi.project.controller.ProjectAllocationDto
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
      assertThat(projectAllocationsDto.allocations[0].projectName).isEqualTo("Community Garden")
      assertThat(projectAllocationsDto.allocations[0].date).isEqualTo(LocalDate.of(2025, 9, 1))
      assertThat(projectAllocationsDto.allocations[0].startTime).isEqualTo(LocalTime.of(9, 0))
      assertThat(projectAllocationsDto.allocations[0].endTime).isEqualTo(LocalTime.of(17, 0))
      assertThat(projectAllocationsDto.allocations[0].projectCode).isEqualTo("cg")
      assertThat(projectAllocationsDto.allocations[0].numberOfOffendersAllocated).isEqualTo(0)
      assertThat(projectAllocationsDto.allocations[0].numberOfOffendersWithOutcomes).isEqualTo(1)
      assertThat(projectAllocationsDto.allocations[0].numberOfOffendersWithEA).isEqualTo(2)

      assertThat(projectAllocationsDto.allocations[1].id).isEqualTo(2L)
      assertThat(projectAllocationsDto.allocations[1].projectName).isEqualTo("Park Cleanup")
      assertThat(projectAllocationsDto.allocations[1].date).isEqualTo(LocalDate.of(2025, 9, 8))
      assertThat(projectAllocationsDto.allocations[1].startTime).isEqualTo(LocalTime.of(8, 0))
      assertThat(projectAllocationsDto.allocations[1].endTime).isEqualTo(LocalTime.of(16, 0))
      assertThat(projectAllocationsDto.allocations[1].projectCode).isEqualTo("pc")
      assertThat(projectAllocationsDto.allocations[1].numberOfOffendersAllocated).isEqualTo(3)
      assertThat(projectAllocationsDto.allocations[1].numberOfOffendersWithOutcomes).isEqualTo(4)
      assertThat(projectAllocationsDto.allocations[1].numberOfOffendersWithEA).isEqualTo(5)
    }

    @Nested
    inner class ProjectAllocationMapper {
      @Test
      fun `should map ProjectAllocation to DTO correctly`() {
        val projectAllocation = ProjectAllocation(
          id = 1L,
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
  }
}
