package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.mappers

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Project
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectAppointmentSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectSession
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectSessionSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectSessionSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.RequirementProgress
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SessionSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.OffenderInfoResult
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toFullAddress
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
            project = ProjectSummary(
              code = "cg",
              name = "Community Garden",
            ),
            date = LocalDate.of(2025, 9, 1),
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(17, 0),
            allocatedCount = 0,
            compliedOutcomeCount = 1,
            enforcementActionNeededCount = 2,
          ),
          ProjectSessionSummary(
            project = ProjectSummary(
              code = "pc",
              name = "Park Cleanup",
            ),
            date = LocalDate.of(2025, 9, 8),
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(16, 0),
            allocatedCount = 3,
            compliedOutcomeCount = 4,
            enforcementActionNeededCount = 5,
          ),
        ),
      )

      val projectAllocationsDto = projectSessions.toDto()

      Assertions.assertThat(projectAllocationsDto.allocations).hasSize(2)

      Assertions.assertThat(projectAllocationsDto.allocations[0].projectName).isEqualTo("Community Garden")
      Assertions.assertThat(projectAllocationsDto.allocations[0].date).isEqualTo(LocalDate.of(2025, 9, 1))
      Assertions.assertThat(projectAllocationsDto.allocations[0].startTime).isEqualTo(LocalTime.of(9, 0))
      Assertions.assertThat(projectAllocationsDto.allocations[0].endTime).isEqualTo(LocalTime.of(17, 0))
      Assertions.assertThat(projectAllocationsDto.allocations[0].projectCode).isEqualTo("cg")
      Assertions.assertThat(projectAllocationsDto.allocations[0].numberOfOffendersAllocated).isEqualTo(0)
      Assertions.assertThat(projectAllocationsDto.allocations[0].numberOfOffendersWithOutcomes).isEqualTo(1)
      Assertions.assertThat(projectAllocationsDto.allocations[0].numberOfOffendersWithEA).isEqualTo(2)

      Assertions.assertThat(projectAllocationsDto.allocations[1].projectName).isEqualTo("Park Cleanup")
      Assertions.assertThat(projectAllocationsDto.allocations[1].date).isEqualTo(LocalDate.of(2025, 9, 8))
      Assertions.assertThat(projectAllocationsDto.allocations[1].startTime).isEqualTo(LocalTime.of(8, 0))
      Assertions.assertThat(projectAllocationsDto.allocations[1].endTime).isEqualTo(LocalTime.of(16, 0))
      Assertions.assertThat(projectAllocationsDto.allocations[1].projectCode).isEqualTo("pc")
      Assertions.assertThat(projectAllocationsDto.allocations[1].numberOfOffendersAllocated).isEqualTo(3)
      Assertions.assertThat(projectAllocationsDto.allocations[1].numberOfOffendersWithOutcomes).isEqualTo(4)
      Assertions.assertThat(projectAllocationsDto.allocations[1].numberOfOffendersWithEA).isEqualTo(5)
    }
  }

  @Nested
  inner class ProjectAllocationMapper {
    @Test
    fun `should map ProjectAllocation to DTO correctly`() {
      val projectAllocation = ProjectSessionSummary(
        project = ProjectSummary(
          code = "cg",
          name = "Community Garden",
        ),
        date = LocalDate.of(2025, 9, 1),
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(17, 0),
        allocatedCount = 40,
        compliedOutcomeCount = 0,
        enforcementActionNeededCount = 0,
      )

      Assertions.assertThat(projectAllocation.toDto()).isEqualTo(
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
          location = uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectLocation(
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
            case = CaseSummary.Companion.valid().copy(crn = "CRN1"),
            requirementProgress = RequirementProgress(
              requirementMinutes = 520,
              completedMinutes = 30,
            ),
          ),
          ProjectAppointmentSummary(
            id = 2L,
            case = CaseSummary.Companion.valid().copy(crn = "CRN2"),
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

      Assertions.assertThat(result.projectName).isEqualTo("Park Cleanup")
      Assertions.assertThat(result.projectCode).isEqualTo("N987654321")
      Assertions.assertThat(result.projectLocation).isEqualTo("The Tower, 1a Somewhere Lane, Guildford, Surrey, AA11 234")
      Assertions.assertThat(result.date).isEqualTo(LocalDate.of(2025, 9, 8))
      Assertions.assertThat(result.startTime).isEqualTo(LocalTime.of(8, 0))
      Assertions.assertThat(result.endTime).isEqualTo(LocalTime.of(16, 0))
      Assertions.assertThat(result.appointmentSummaries).hasSize(2)
      Assertions.assertThat(result.appointmentSummaries[0].id).isEqualTo(1L)
      Assertions.assertThat(result.appointmentSummaries[0].requirementMinutes).isEqualTo(520)
      Assertions.assertThat(result.appointmentSummaries[0].completedMinutes).isEqualTo(30)
      Assertions.assertThat(result.appointmentSummaries[0].offender).isNotNull

      Assertions.assertThat(result.appointmentSummaries[1].id).isEqualTo(2L)
      Assertions.assertThat(result.appointmentSummaries[1].requirementMinutes).isEqualTo(20)
      Assertions.assertThat(result.appointmentSummaries[1].completedMinutes).isEqualTo(10)
      Assertions.assertThat(result.appointmentSummaries[1].offender).isNotNull
    }
  }

  @Nested
  inner class ProjectLocation {

    @Test
    fun `empty location mapped to empty string`() {
      Assertions.assertThat(
        uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectLocation(
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
      Assertions.assertThat(
        uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectLocation(
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
      Assertions.assertThat(
        uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectLocation(
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
