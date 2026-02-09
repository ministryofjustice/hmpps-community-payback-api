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
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAppointmentSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDContactOutcome
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProjectAndLocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProjectSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSession
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSessionSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSessionSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SessionSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.AppointmentMappers
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.SessionMappers
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toFullAddress
import java.time.LocalDate
import java.time.LocalTime

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
      assertThat(projectAllocationsDto.allocations[0].startTime).isEqualTo(LocalTime.of(0, 0))
      assertThat(projectAllocationsDto.allocations[0].endTime).isEqualTo(LocalTime.of(0, 0))
      assertThat(projectAllocationsDto.allocations[0].projectCode).isEqualTo("cg")
      assertThat(projectAllocationsDto.allocations[0].numberOfOffendersAllocated).isEqualTo(0)
      assertThat(projectAllocationsDto.allocations[0].numberOfOffendersWithOutcomes).isEqualTo(1)
      assertThat(projectAllocationsDto.allocations[0].numberOfOffendersWithEA).isEqualTo(2)

      assertThat(projectAllocationsDto.allocations[1].projectName).isEqualTo("Park Cleanup")
      assertThat(projectAllocationsDto.allocations[1].date).isEqualTo(LocalDate.of(2025, 9, 8))
      assertThat(projectAllocationsDto.allocations[1].startTime).isEqualTo(LocalTime.of(0, 0))
      assertThat(projectAllocationsDto.allocations[1].endTime).isEqualTo(LocalTime.of(0, 0))
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
          startTime = LocalTime.of(0, 0),
          endTime = LocalTime.of(0, 0),
          projectCode = "cg",
          numberOfOffendersAllocated = 40,
          numberOfOffendersWithOutcomes = 0,
          numberOfOffendersWithEA = 0,
        ),
      )
    }
  }

  @Nested
  inner class SessionToSessionDto {
    @Test
    fun `should map ProjectSession to DTO correctly`() {
      val appointmentSummary1 = NDAppointmentSummary.valid().copy(case = NDCaseSummary.valid().copy(crn = "CRN1"))
      val appointmentSummary2 = NDAppointmentSummary.valid().copy(case = NDCaseSummary.valid().copy(crn = "CRN2"))

      val appointmentSummaryDto1 = AppointmentSummaryDto.valid()
      val appointmentSummaryDto2 = AppointmentSummaryDto.valid()
      every { appointmentMappers.toSummaryDto(appointmentSummary1) } returns appointmentSummaryDto1
      every { appointmentMappers.toSummaryDto(appointmentSummary2) } returns appointmentSummaryDto2

      val session = NDSession(
        project = NDProjectAndLocation(
          name = "Park Cleanup",
          code = "N987654321",
          location = NDAddress(
            buildingName = "The Tower",
            addressNumber = "1a",
            streetName = "Somewhere Lane",
            townCity = "Guildford",
            county = "Surrey",
            postCode = "AA11 234",
          ),
        ),
        appointmentSummaries = listOf(appointmentSummary1, appointmentSummary2),
      )

      val result = service.toDto(
        date = LocalDate.of(2025, 9, 8),
        session = session,
      )

      assertThat(result.projectName).isEqualTo("Park Cleanup")
      assertThat(result.projectCode).isEqualTo("N987654321")
      assertThat(result.projectLocation).isEqualTo("The Tower, 1a Somewhere Lane, Guildford, Surrey, AA11 234")
      assertThat(result.location.buildingName).isEqualTo("The Tower")
      assertThat(result.location.buildingNumber).isEqualTo("1a")
      assertThat(result.location.streetName).isEqualTo("Somewhere Lane")
      assertThat(result.location.townCity).isEqualTo("Guildford")
      assertThat(result.location.county).isEqualTo("Surrey")
      assertThat(result.location.postCode).isEqualTo("AA11 234")

      assertThat(result.date).isEqualTo(LocalDate.of(2025, 9, 8))
      assertThat(result.appointmentSummaries).isEqualTo(listOf(appointmentSummaryDto1, appointmentSummaryDto2))
    }
  }

  @Nested
  inner class SessionToSessionSummaryDto {

    @Test
    fun `Should map correctly`() {
      val session = NDSession(
        project = NDProjectAndLocation(
          name = "Park Cleanup",
          code = "N987654321",
          location = NDAddress.valid(),
        ),
        appointmentSummaries = listOf(
          NDAppointmentSummary.valid().copy(outcome = NDContactOutcome.valid().copy(code = "ATTEND-1")),
          NDAppointmentSummary.valid().copy(outcome = NDContactOutcome.valid().copy(code = "ATTEND-1")),
          NDAppointmentSummary.valid().copy(outcome = NDContactOutcome.valid().copy(code = "ATTEND-2")),
          NDAppointmentSummary.valid().copy(outcome = NDContactOutcome.valid().copy(code = "ENFORCE-1")),
          NDAppointmentSummary.valid().copy(outcome = NDContactOutcome.valid().copy(code = "ENFORCE-2")),
          NDAppointmentSummary.valid().copy(outcome = null),
          NDAppointmentSummary.valid().copy(outcome = null),
        ),
      )

      every { contactOutcomeEntityRepository.findByCode("ATTEND-1") } returns ContactOutcomeEntity.valid().copy(attended = true, enforceable = false)
      every { contactOutcomeEntityRepository.findByCode("ATTEND-2") } returns ContactOutcomeEntity.valid().copy(attended = true, enforceable = false)
      every { contactOutcomeEntityRepository.findByCode("ENFORCE-1") } returns ContactOutcomeEntity.valid().copy(attended = false, enforceable = true)
      every { contactOutcomeEntityRepository.findByCode("ENFORCE-2") } returns ContactOutcomeEntity.valid().copy(attended = false, enforceable = true)

      val result = service.toSummaryDto(
        date = LocalDate.of(2025, 9, 8),
        session = session,
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
