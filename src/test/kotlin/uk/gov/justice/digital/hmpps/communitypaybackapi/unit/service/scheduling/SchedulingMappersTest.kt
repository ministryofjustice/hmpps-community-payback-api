package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.scheduling

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Code
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCodeDescription
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDNameCode
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDPickUp
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDRequirementProgress
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingAllocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingAvailability
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingDayOfWeek
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingExistingAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingFrequency
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.scheduling.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingAllocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingFrequency
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingRequiredAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.toCreateAppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.toSchedulingAllocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.toSchedulingAllocations
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.toSchedulingExistingAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.toSchedulingRequirement
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class SchedulingMappersTest {

  @Nested
  inner class BuildSchedulingRequirement {

    @Test
    fun success() {
      val result = NDRequirementProgress(
        requiredMinutes = 100,
        completedMinutes = 25,
        adjustments = -10,
      ).toSchedulingRequirement(
        crn = "CRN1",
        eventNumber = 2,
      )

      assertThat(result.requirementLengthMinutes).isEqualTo(Duration.ofMinutes(90))
      assertThat(result.crn).isEqualTo("CRN1")
      assertThat(result.deliusEventNumber).isEqualTo(2)
    }
  }

  @Nested
  inner class NDSchedulingExistingAppointmentToSchedulingExistingAppointment {

    @Test
    fun `all fields populated`() {
      val result = NDSchedulingExistingAppointment(
        id = Long.random(),
        project = NDNameCode.valid().copy(
          code = "PROJ1",
        ),
        date = LocalDate.of(2025, 2, 3),
        startTime = LocalTime.of(1, 2),
        endTime = LocalTime.of(4, 5),
        outcome = NDCodeDescription("OUTCOME1", "Description"),
        minutesCredited = 65,
        allocationId = 12L,
      ).toSchedulingExistingAppointment()

      assertThat(result.projectCode).isEqualTo("PROJ1")
      assertThat(result.date).isEqualTo(LocalDate.of(2025, 2, 3))
      assertThat(result.startTime).isEqualTo(LocalTime.of(1, 2))
      assertThat(result.endTime).isEqualTo(LocalTime.of(4, 5))
      assertThat(result.hasOutcome).isTrue
      assertThat(result.minutesCredited).isEqualTo(Duration.ofMinutes(65))
      assertThat(result.allocationId).isEqualTo(12L)
    }

    @Test
    fun `only mandatory fields populated`() {
      val result = NDSchedulingExistingAppointment(
        id = Long.random(),
        project = NDNameCode.valid().copy(
          code = "PROJ1",
        ),
        date = LocalDate.of(2025, 2, 3),
        startTime = LocalTime.of(1, 2),
        endTime = LocalTime.of(4, 5),
        outcome = null,
        minutesCredited = null,
        allocationId = null,
      ).toSchedulingExistingAppointment()

      assertThat(result.projectCode).isEqualTo("PROJ1")
      assertThat(result.date).isEqualTo(LocalDate.of(2025, 2, 3))
      assertThat(result.startTime).isEqualTo(LocalTime.of(1, 2))
      assertThat(result.endTime).isEqualTo(LocalTime.of(4, 5))
      assertThat(result.hasOutcome).isFalse
      assertThat(result.minutesCredited).isNull()
      assertThat(result.allocationId).isNull()
    }
  }

  @Nested
  inner class NDSchedulingAllocationsToSchedulingAllocations {

    @Test
    fun `Ignore allocations where startDate == endDate`() {
      val alloc1 = NDSchedulingAllocation.valid().copy(
        startDateInclusive = LocalDate.of(2025, 2, 3),
        endDateInclusive = LocalDate.of(2025, 2, 4),
      )
      val alloc2 = NDSchedulingAllocation.valid().copy(
        startDateInclusive = LocalDate.of(2025, 2, 3),
        endDateInclusive = LocalDate.of(2025, 2, 3),
      )
      val alloc3 = NDSchedulingAllocation.valid().copy(
        startDateInclusive = LocalDate.of(2025, 1, 1),
        endDateInclusive = null,
      )

      val result = listOf(alloc1, alloc2, alloc3).toSchedulingAllocations().allocations

      assertThat(result.map { it.id }).containsExactlyInAnyOrder(alloc1.id, alloc3.id)
    }

    @Test
    fun `Ignore allocations where startTime == endTime`() {
      val alloc1 = NDSchedulingAllocation.valid().copy(
        startDateInclusive = LocalDate.of(2025, 2, 3),
        endDateInclusive = null,
        startTime = LocalTime.of(0, 0),
        endTime = LocalTime.of(0, 0),
      )
      val alloc2 = NDSchedulingAllocation.valid().copy(
        startDateInclusive = LocalDate.of(2025, 2, 3),
        endDateInclusive = null,
        startTime = LocalTime.of(0, 0),
        endTime = LocalTime.of(23, 59),
      )
      val alloc3 = NDSchedulingAllocation.valid().copy(
        startDateInclusive = LocalDate.of(2025, 1, 1),
        endDateInclusive = null,
        startTime = LocalTime.of(0, 0),
        endTime = LocalTime.of(0, 1),
      )

      val result = listOf(alloc1, alloc2, alloc3).toSchedulingAllocations().allocations

      assertThat(result.map { it.id }).containsExactlyInAnyOrder(alloc2.id, alloc3.id)
    }
  }

  @Nested
  inner class NDSchedulingAllocationToSchedulingAllocation {

    @Test
    fun `all fields populated`() {
      val result = NDSchedulingAllocation.valid().copy(
        id = 1234L,
        project = NDSchedulingProject.valid().copy(
          code = "PROJ1",
        ),
        projectAvailability = NDSchedulingAvailability.valid().copy(
          frequency = null,
        ),
        frequency = NDSchedulingFrequency.Once,
        dayOfWeek = NDSchedulingDayOfWeek.Monday,
        startDateInclusive = LocalDate.of(2021, 2, 3),
        endDateInclusive = LocalDate.of(2022, 4, 5),
        startTime = LocalTime.of(1, 2),
        endTime = LocalTime.of(3, 4),
        pickUp = NDPickUp(
          time = LocalTime.of(5, 6),
          location = Code("PICKUPLOC1"),
        ),
      ).toSchedulingAllocation()

      assertThat(result.id).isEqualTo(1234)
      assertThat(result.alias).isEqualTo("ALLOC 1234")
      assertThat(result.project.code).isEqualTo("PROJ1")
      assertThat(result.frequency).isEqualTo(SchedulingFrequency.ONCE)
      assertThat(result.dayOfWeek).isEqualTo(DayOfWeek.MONDAY)
      assertThat(result.startDateInclusive).isEqualTo(LocalDate.of(2021, 2, 3))
      assertThat(result.endDateInclusive).isEqualTo(LocalDate.of(2022, 4, 5))
      assertThat(result.startTime).isEqualTo(LocalTime.of(1, 2))
      assertThat(result.endTime).isEqualTo(LocalTime.of(3, 4))
      assertThat(result.pickUpLocationCode).isEqualTo("PICKUPLOC1")
      assertThat(result.pickUpTime).isEqualTo(LocalTime.of(5, 6))
    }

    @Test
    fun `only mandatory fields populated`() {
      val result = NDSchedulingAllocation.valid().copy(
        id = 1234L,
        project = NDSchedulingProject.valid().copy(
          code = "PROJ1",
          expectedEndDateExclusive = null,
          actualEndDateExclusive = null,
        ),
        projectAvailability = null,
        frequency = null,
        dayOfWeek = NDSchedulingDayOfWeek.Tuesday,
        startDateInclusive = LocalDate.of(2021, 2, 3),
        endDateInclusive = null,
        startTime = LocalTime.of(1, 2),
        endTime = LocalTime.of(3, 4),
        pickUp = null,
      ).toSchedulingAllocation()

      assertThat(result.id).isEqualTo(1234)
      assertThat(result.alias).isEqualTo("ALLOC 1234")
      assertThat(result.project.code).isEqualTo("PROJ1")
      assertThat(result.frequency).isEqualTo(SchedulingFrequency.WEEKLY)
      assertThat(result.dayOfWeek).isEqualTo(DayOfWeek.TUESDAY)
      assertThat(result.startDateInclusive).isEqualTo(LocalDate.of(2021, 2, 3))
      assertThat(result.endDateInclusive).isNull()
      assertThat(result.startTime).isEqualTo(LocalTime.of(1, 2))
      assertThat(result.endTime).isEqualTo(LocalTime.of(3, 4))
      assertThat(result.pickUpLocationCode).isNull()
      assertThat(result.pickUpTime).isNull()
    }

    @ParameterizedTest
    @CsvSource(
      nullValues = ["null"],
      value = [
        "null,null,WEEKLY",
        "Once,Once,ONCE",
        "Once,null,ONCE",
        "null,Once,ONCE",
        "Weekly,Weekly,WEEKLY",
        "null,Weekly,WEEKLY",
        "Weekly,null,WEEKLY",
        "Fortnightly,Fortnightly,FORTNIGHTLY",
        "Fortnightly,null,FORTNIGHTLY",
        "null,Fortnightly,FORTNIGHTLY",
        "Fortnightly,Weekly,FORTNIGHTLY",
        "Weekly,Fortnightly,FORTNIGHTLY",
      ],
    )
    fun `frequency correctly derived`(
      availabilityFreq: NDSchedulingFrequency?,
      allocationFreq: NDSchedulingFrequency?,
      expectedFrequency: SchedulingFrequency,
    ) {
      val result = NDSchedulingAllocation.valid().copy(
        projectAvailability = NDSchedulingAvailability.valid().copy(
          frequency = availabilityFreq,
        ),
        frequency = allocationFreq,
      ).toSchedulingAllocation()

      assertThat(result.frequency).isEqualTo(expectedFrequency)
    }

    @ParameterizedTest
    @CsvSource(
      nullValues = ["null"],
      value = [
        "null,null,null,null,null",
        "2025-01-01,null,null,null,2025-01-01",
        "2025-01-01,2024-01-01,2024-01-01,2024-01-01,2025-01-01",
        "null,2022-01-02,2023-01-02,2024-01-02,2022-01-01",
        "null,2023-01-02,2022-01-02,2024-01-02,2022-01-01",
        "null,2023-01-02,2024-01-02,2022-01-02,2022-01-01",
      ],
    )
    fun `end date correctly derived`(
      allocationEndDateInclusive: LocalDate?,
      projectExpectedEndDateExclusive: LocalDate?,
      projectActualEndDateExclusive: LocalDate?,
      availabilityEndDateExclusive: LocalDate?,
      expectedEndDateInclusive: LocalDate?,
    ) {
      val result = NDSchedulingAllocation.valid().copy(
        project = NDSchedulingProject.valid().copy(
          expectedEndDateExclusive = projectExpectedEndDateExclusive,
          actualEndDateExclusive = projectActualEndDateExclusive,
        ),
        projectAvailability = NDSchedulingAvailability.valid().copy(
          endDateExclusive = availabilityEndDateExclusive,
        ),
        endDateInclusive = allocationEndDateInclusive,
      ).toSchedulingAllocation()

      assertThat(result.endDateInclusive).isEqualTo(expectedEndDateInclusive)
    }
  }

  @Nested
  inner class SchedulingRequiredAppointmentToCreateAppointmentDto {

    @Test
    fun `map all fields`() {
      val result = SchedulingRequiredAppointment(
        id = UUID.randomUUID(),
        date = LocalDate.of(2021, 9, 8),
        startTime = LocalTime.of(1, 2),
        endTime = LocalTime.of(11, 12),
        project = SchedulingProject.valid().copy(
          code = "P1",
        ),
        allocation = SchedulingAllocation.valid().copy(
          id = 55L,
          pickUpLocationCode = "PICKUP1",
          pickUpTime = LocalTime.of(13, 14),
        ),
      ).toCreateAppointmentDto(
        crn = "CRN1",
        eventNumber = 25,
      )

      assertThat(result.crn).isEqualTo("CRN1")
      assertThat(result.deliusEventNumber).isEqualTo(25)
      assertThat(result.allocationId).isEqualTo(55)
      assertThat(result.date).isEqualTo(LocalDate.of(2021, 9, 8))
      assertThat(result.startTime).isEqualTo(LocalTime.of(1, 2))
      assertThat(result.endTime).isEqualTo(LocalTime.of(11, 12))
      assertThat(result.pickUpLocationCode).isEqualTo("PICKUP1")
      assertThat(result.pickUpTime).isEqualTo(LocalTime.of(13, 14))
      assertThat(result.contactOutcomeCode).isNull()
      assertThat(result.attendanceData).isNull()
      assertThat(result.supervisorOfficerCode).isNull()
      assertThat(result.notes).isEqualTo("[System scheduled appointment]")
      assertThat(result.sensitive).isNull()
      assertThat(result.alertActive).isNull()
    }
  }
}
