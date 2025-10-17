package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.project.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.Project
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAppointmentSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectLocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectSession
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectSessionSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectSessionSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.RequirementProgress
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.OffenderInfoResult
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.project.dto.SessionSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.project.service.toDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.project.service.toFullAddress
import java.time.LocalDate
import java.time.LocalTime

class ProjectMappersTest {

  @Nested
  inner class ProjectAllocationsMapper {

    @Test
    fun `should map ProjectAllocations to DTO correctly`() {
      val projectSessions = ProjectSessionSummaries(
        listOf(
          ProjectSessionSummary(
            projectName = "Community Garden",
            date = LocalDate.of(2025, 9, 1),
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(17, 0),
            projectCode = "cg",
            allocatedCount = 0,
            compliedOutcomeCount = 1,
            enforcementActionNeededCount = 2,
          ),
          ProjectSessionSummary(
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
      val projectAllocation = ProjectSessionSummary(
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
        project = Project(
          name = "Park Cleanup",
          code = "N987654321",
          location = ProjectLocation(
            buildingName = "The Tower",
            addressNumber = "1a",
            streetName = "Somewhere Lane",
            townCity = "Guildford",
            county = "Surrey",
            postCode = "AA11 234",
          ),
        ),
        date = LocalDate.of(2025, 9, 8),
        startTime = LocalTime.of(8, 0),
        endTime = LocalTime.of(16, 0),
        appointmentSummaries = listOf(
          ProjectAppointmentSummary(
            id = 1L,
            case = CaseSummary.valid().copy(crn = "CRN1"),
            requirementProgress = RequirementProgress(
              requirementMinutes = 520,
              completedMinutes = 30,
            ),
          ),
          ProjectAppointmentSummary(
            id = 2L,
            case = CaseSummary.valid().copy(crn = "CRN2"),
            requirementProgress = RequirementProgress(
              requirementMinutes = 20,
              completedMinutes = 10,
            ),
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
      assertThat(result.projectLocation).isEqualTo("The Tower, 1a Somewhere Lane, Guildford, Surrey, AA11 234")
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
  inner class ProjectLocation {

    @Test
    fun `empty location mapped to empty string`() {
      assertThat(
        ProjectLocation(
          buildingName = null,
          addressNumber = null,
          streetName = null,
          townCity = null,
          county = null,
          postCode = null,
        ).toFullAddress(),
      ).isEqualTo("")
    }

    @Test
    fun `no address number`() {
      assertThat(
        ProjectLocation(
          buildingName = "building",
          addressNumber = null,
          streetName = "street",
          townCity = "townCity",
          county = null,
          postCode = "postcode",
        ).toFullAddress(),
      ).isEqualTo("building, street, townCity, postcode")
    }

    @Test
    fun `all fields provided`() {
      assertThat(
        ProjectLocation(
          buildingName = "building",
          addressNumber = "address",
          streetName = "street",
          townCity = "townCity",
          county = "county",
          postCode = "postcode",
        ).toFullAddress(),
      ).isEqualTo("building, address street, townCity, county, postcode")
    }
  }
}
