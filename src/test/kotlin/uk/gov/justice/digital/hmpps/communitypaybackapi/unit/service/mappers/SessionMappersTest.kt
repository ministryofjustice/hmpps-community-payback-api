package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.mappers

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAddress
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProjectSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSessionSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSessionSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ContactOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SessionSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.AppointmentMappers
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.SessionMappers
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toFullAddress
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class SessionMappersTest {

  @MockK(relaxed = true)
  private lateinit var appointmentMappers: AppointmentMappers

  @MockK(relaxed = true)
  private lateinit var contactOutcomeEntityRepository: ContactOutcomeEntityRepository

  @InjectMockKs
  private lateinit var service: SessionMappers

  @Nested
  inner class SessionSummariesToSessionSummariesDto {

    @Test
    fun `should map ProjectAllocations to DTO correctly`() {
      val projectSessions = NDSessionSummaries(
        listOf(
          NDSessionSummary(
            project = NDProjectSummary(
              code = "cg",
              description = "Community Garden",
            ),
            date = LocalDate.of(2025, 9, 1),
            allocatedCount = 0,
            outcomeCount = 1,
            enforcementActionCount = 2,
          ),
          NDSessionSummary(
            project = NDProjectSummary(
              code = "pc",
              description = "Park Cleanup",
            ),
            date = LocalDate.of(2025, 9, 8),
            allocatedCount = 3,
            outcomeCount = 4,
            enforcementActionCount = 5,
          ),
        ),
      )

      val projectAllocationsDto = projectSessions.toDto()

      assertThat(projectAllocationsDto.allocations).hasSize(2)

      assertThat(projectAllocationsDto.allocations[0].projectName).isEqualTo("Community Garden")
      assertThat(projectAllocationsDto.allocations[0].date).isEqualTo(LocalDate.of(2025, 9, 1))
      assertThat(projectAllocationsDto.allocations[0].projectCode).isEqualTo("cg")
      assertThat(projectAllocationsDto.allocations[0].numberOfOffendersAllocated).isEqualTo(0)
      assertThat(projectAllocationsDto.allocations[0].numberOfOffendersWithOutcomes).isEqualTo(1)
      assertThat(projectAllocationsDto.allocations[0].numberOfOffendersWithEA).isEqualTo(2)

      assertThat(projectAllocationsDto.allocations[1].projectName).isEqualTo("Park Cleanup")
      assertThat(projectAllocationsDto.allocations[1].date).isEqualTo(LocalDate.of(2025, 9, 8))
      assertThat(projectAllocationsDto.allocations[1].projectCode).isEqualTo("pc")
      assertThat(projectAllocationsDto.allocations[1].numberOfOffendersAllocated).isEqualTo(3)
      assertThat(projectAllocationsDto.allocations[1].numberOfOffendersWithOutcomes).isEqualTo(4)
      assertThat(projectAllocationsDto.allocations[1].numberOfOffendersWithEA).isEqualTo(5)
    }
  }

  @Nested
  inner class SessionSummaryToSessionSummaryDto {
    @Test
    fun `should map ProjectAllocation to DTO correctly`() {
      val projectAllocation = NDSessionSummary(
        project = NDProjectSummary(
          code = "cg",
          description = "Community Garden",
        ),
        date = LocalDate.of(2025, 9, 1),
        allocatedCount = 40,
        outcomeCount = 0,
        enforcementActionCount = 0,
      )

      assertThat(projectAllocation.toDto()).isEqualTo(
        SessionSummaryDto(
          projectName = "Community Garden",
          date = LocalDate.of(2025, 9, 1),
          projectCode = "cg",
          numberOfOffendersAllocated = 40,
          numberOfOffendersWithOutcomes = 0,
          numberOfOffendersWithEA = 0,
        ),
      )
    }
  }

  @Nested
  inner class ProjectAndAppointmentsToSessionDto {
    @Test
    fun `should map project and appointments to sessions correctly`() {
      val project = ProjectDto.valid().copy(
        projectCode = "PC1",
        projectName = "Project Name 1",
      )
      val appointments = listOf(
        AppointmentSummaryDto.valid(),
        AppointmentSummaryDto.valid(),
      )

      val result = service.toSessionDto(
        date = LocalDate.of(2025, 9, 1),
        project = project,
        appointments = appointments,
      )

      assertThat(result.projectCode).isEqualTo("PC1")
      assertThat(result.projectName).isEqualTo("Project Name 1")
      assertThat(result.location).isEqualTo(project.location)
      assertThat(result.date).isEqualTo(LocalDate.of(2025, 9, 1))
      assertThat(result.appointmentSummaries).isEqualTo(appointments)

      assertThat(result.projectLocation).isEqualTo("")
    }
  }

  @Nested
  inner class SessionToSessionSummaryDto {

    @Test
    fun `Should map correctly`() {
      val project = ProjectDto.valid().copy(
        projectCode = "N987654321",
        projectName = "Park Cleanup",
      )
      val appointments = listOf(
        AppointmentSummaryDto.valid().copy(contactOutcome = ContactOutcomeDto.valid().copy(code = "ATTEND-1")),
        AppointmentSummaryDto.valid().copy(contactOutcome = ContactOutcomeDto.valid().copy(code = "ATTEND-1")),
        AppointmentSummaryDto.valid().copy(contactOutcome = ContactOutcomeDto.valid().copy(code = "ATTEND-2")),
        AppointmentSummaryDto.valid().copy(contactOutcome = ContactOutcomeDto.valid().copy(code = "ENFORCE-1")),
        AppointmentSummaryDto.valid().copy(contactOutcome = ContactOutcomeDto.valid().copy(code = "ENFORCE-1")),
        AppointmentSummaryDto.valid().copy(contactOutcome = null),
        AppointmentSummaryDto.valid().copy(contactOutcome = null),
      )

      every { contactOutcomeEntityRepository.findByCode("ATTEND-1") } returns ContactOutcomeEntity.valid().copy(attended = true, enforceable = false)
      every { contactOutcomeEntityRepository.findByCode("ATTEND-2") } returns ContactOutcomeEntity.valid().copy(attended = true, enforceable = false)
      every { contactOutcomeEntityRepository.findByCode("ENFORCE-1") } returns ContactOutcomeEntity.valid().copy(attended = false, enforceable = true)
      every { contactOutcomeEntityRepository.findByCode("ENFORCE-2") } returns ContactOutcomeEntity.valid().copy(attended = false, enforceable = true)

      val result = service.toSummaryDto(
        date = LocalDate.of(2025, 9, 8),
        project = project,
        appointments = appointments,
      )

      assertThat(result.projectCode).isEqualTo("N987654321")
      assertThat(result.projectName).isEqualTo("Park Cleanup")
      assertThat(result.date).isEqualTo(LocalDate.of(2025, 9, 8))
      assertThat(result.numberOfOffendersAllocated).isEqualTo(7)
      assertThat(result.numberOfOffendersWithOutcomes).isEqualTo(5)
      assertThat(result.numberOfOffendersWithEA).isEqualTo(2)
    }
  }

  @Nested
  inner class ProjectLocationToFullAddress {

    @Test
    fun `empty location mapped to empty string`() {
      assertThat(
        NDAddress(
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
        NDAddress(
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
        NDAddress(
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
