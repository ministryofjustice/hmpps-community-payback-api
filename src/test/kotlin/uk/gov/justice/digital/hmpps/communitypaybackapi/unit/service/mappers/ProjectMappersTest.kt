package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.mappers

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Project
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectAppointmentSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectLocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectSession
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectSessionSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectSessionSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SessionSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.OffenderInfoResult
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.AppointmentMappers
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.ProjectMappers
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toFullAddress
import java.time.LocalDate
import java.time.LocalTime

@ExtendWith(MockKExtension::class)
class ProjectMappersTest {

  @MockK(relaxed = true)
  private lateinit var appointmentMappers: AppointmentMappers

  @InjectMockKs
  private lateinit var service: ProjectMappers

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
      val appointmentSummary1 = ProjectAppointmentSummary.valid().copy(case = CaseSummary.valid().copy(crn = "CRN1"))
      val appointmentSummary2 = ProjectAppointmentSummary.valid().copy(case = CaseSummary.valid().copy(crn = "CRN2"))

      val offenderInfoResult1 = OffenderInfoResult.Limited("CRN1")
      val offenderInfoResult2 = OffenderInfoResult.Limited("CRN2")

      val appointmentSummaryDto1 = AppointmentSummaryDto.valid()
      val appointmentSummaryDto2 = AppointmentSummaryDto.valid()
      every { appointmentMappers.toDto(appointmentSummary1, offenderInfoResult1) } returns appointmentSummaryDto1
      every { appointmentMappers.toDto(appointmentSummary2, offenderInfoResult2) } returns appointmentSummaryDto2

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
        appointmentSummaries = listOf(appointmentSummary1, appointmentSummary2),
      )

      val result = service.toDto(
        projectSession,
        offenderInfoResults = listOf(
          offenderInfoResult1,
          offenderInfoResult2,
        ),
      )

      assertThat(result.projectName).isEqualTo("Park Cleanup")
      assertThat(result.projectCode).isEqualTo("N987654321")
      assertThat(result.projectLocation).isEqualTo("The Tower, 1a Somewhere Lane, Guildford, Surrey, AA11 234")
      assertThat(result.location.buildingName).isEqualTo("The Tower")
      assertThat(result.location.addressNumber).isEqualTo("1a")
      assertThat(result.location.streetName).isEqualTo("Somewhere Lane")
      assertThat(result.location.townCity).isEqualTo("Guildford")
      assertThat(result.location.county).isEqualTo("Surrey")
      assertThat(result.location.postCode).isEqualTo("AA11 234")

      assertThat(result.date).isEqualTo(LocalDate.of(2025, 9, 8))
      assertThat(result.startTime).isEqualTo(LocalTime.of(8, 0))
      assertThat(result.endTime).isEqualTo(LocalTime.of(16, 0))
      assertThat(result.appointmentSummaries).isEqualTo(listOf(appointmentSummaryDto1, appointmentSummaryDto2))
    }
  }

  @Nested
  inner class ProjectLocationMapper {

    @Test
    fun `empty location mapped to empty string`() {
      assertThat(
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
      assertThat(
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
      assertThat(
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
