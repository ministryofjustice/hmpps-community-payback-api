package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.scheduling

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Code
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingAllocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingAvailability
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingDayOfWeek
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingFrequency
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.RequirementProgress
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.scheduling.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingAllocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingFrequency
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingRequiredAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.toNDCreateAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.toSchedulingAllocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.toSchedulingAllocations
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.toSchedulingExistingAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.toSchedulingRequirementProgress
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class SchedulingMappersTest {

  @Nested
  inner class RequirementProgressToSchedulingRequirement {

    @Test
    fun success() {
      val result = RequirementProgress(
        requiredMinutes = 100,
        completedMinutes = 25,
        adjustments = -10,
      ).toSchedulingRequirementProgress()

      assertThat(result.lengthMinutes).isEqualTo(Duration.ofMinutes(65))
    }
  }

  @Nested
  inner class NDSchedulingAppointmentToSchedulingExistingAppointment {

    @Test
    fun `all fields populated`() {
      val result = NDSchedulingAppointment(
        id = UUID.randomUUID(),
        project = NDSchedulingProject.valid().copy(
          code = Code("PROJ1"),
        ),
        date = LocalDate.of(2025, 2, 3),
        startTime = LocalTime.of(1, 2),
        endTime = LocalTime.of(4, 5),
        outcome = Code("OUTCOME1"),
        timeCredited = Duration.ofMinutes(65),
        allocation = NDSchedulingAllocation.valid(),
      ).toSchedulingExistingAppointment()

      assertThat(result.project.code).isEqualTo("PROJ1")
      assertThat(result.date).isEqualTo(LocalDate.of(2025, 2, 3))
      assertThat(result.startTime).isEqualTo(LocalTime.of(1, 2))
      assertThat(result.endTime).isEqualTo(LocalTime.of(4, 5))
      assertThat(result.hasOutcome).isTrue
      assertThat(result.timeCredited).isEqualTo(Duration.ofMinutes(65))
      assertThat(result.allocation).isNotNull
    }

    @Test
    fun `only mandatory fields populated`() {
      val result = NDSchedulingAppointment(
        id = UUID.randomUUID(),
        project = NDSchedulingProject.valid().copy(
          code = Code("PROJ1"),
        ),
        date = LocalDate.of(2025, 2, 3),
        startTime = LocalTime.of(1, 2),
        endTime = LocalTime.of(4, 5),
        outcome = null,
        timeCredited = null,
        allocation = null,
      ).toSchedulingExistingAppointment()

      assertThat(result.project.code).isEqualTo("PROJ1")
      assertThat(result.date).isEqualTo(LocalDate.of(2025, 2, 3))
      assertThat(result.startTime).isEqualTo(LocalTime.of(1, 2))
      assertThat(result.endTime).isEqualTo(LocalTime.of(4, 5))
      assertThat(result.hasOutcome).isFalse
      assertThat(result.timeCredited).isNull()
      assertThat(result.allocation).isNull()
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
          code = Code("PROJ1"),
        ),
        projectAvailability = NDSchedulingAvailability.valid().copy(
          frequency = null,
        ),
        frequency = NDSchedulingFrequency.ONCE,
        dayOfWeek = NDSchedulingDayOfWeek.MONDAY,
        startDateInclusive = LocalDate.of(2021, 2, 3),
        endDateInclusive = LocalDate.of(2022, 4, 5),
        startTime = LocalTime.of(1, 2),
        endTime = LocalTime.of(3, 4),
      ).toSchedulingAllocation()

      assertThat(result.id).isEqualTo(1234)
      assertThat(result.alias).isEqualTo("ALLOC1234")
      assertThat(result.project.code).isEqualTo("PROJ1")
      assertThat(result.frequency).isEqualTo(SchedulingFrequency.ONCE)
      assertThat(result.dayOfWeek).isEqualTo(DayOfWeek.MONDAY)
      assertThat(result.startDateInclusive).isEqualTo(LocalDate.of(2021, 2, 3))
      assertThat(result.endDateInclusive).isEqualTo(LocalDate.of(2022, 4, 5))
      assertThat(result.startTime).isEqualTo(LocalTime.of(1, 2))
      assertThat(result.endTime).isEqualTo(LocalTime.of(3, 4))
    }

    @Test
    fun `only mandatory fields populated`() {
      val result = NDSchedulingAllocation.valid().copy(
        id = 1234L,
        project = NDSchedulingProject.valid().copy(
          code = Code("PROJ1"),
          expectedEndDateExclusive = null,
          actualEndDateExclusive = null,
        ),
        projectAvailability = null,
        frequency = null,
        dayOfWeek = NDSchedulingDayOfWeek.TUESDAY,
        startDateInclusive = LocalDate.of(2021, 2, 3),
        endDateInclusive = null,
        startTime = LocalTime.of(1, 2),
        endTime = LocalTime.of(3, 4),
      ).toSchedulingAllocation()

      assertThat(result.id).isEqualTo(1234)
      assertThat(result.alias).isEqualTo("ALLOC1234")
      assertThat(result.project.code).isEqualTo("PROJ1")
      assertThat(result.frequency).isEqualTo(SchedulingFrequency.WEEKLY)
      assertThat(result.dayOfWeek).isEqualTo(DayOfWeek.TUESDAY)
      assertThat(result.startDateInclusive).isEqualTo(LocalDate.of(2021, 2, 3))
      assertThat(result.endDateInclusive).isNull()
      assertThat(result.startTime).isEqualTo(LocalTime.of(1, 2))
      assertThat(result.endTime).isEqualTo(LocalTime.of(3, 4))
    }

    @ParameterizedTest
    @CsvSource(
      nullValues = ["null"],
      value = [
        "null,null,WEEKLY",
        "ONCE,ONCE,ONCE",
        "ONCE,null,ONCE",
        "null,ONCE,ONCE",
        "WEEKLY,WEEKLY,WEEKLY",
        "null,WEEKLY,WEEKLY",
        "WEEKLY,null,WEEKLY",
        "FORTNIGHTLY,FORTNIGHTLY,FORTNIGHTLY",
        "FORTNIGHTLY,null,FORTNIGHTLY",
        "null,FORTNIGHTLY,FORTNIGHTLY",
        "FORTNIGHTLY,WEEKLY,FORTNIGHTLY",
        "WEEKLY,FORTNIGHTLY,FORTNIGHTLY",
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
  inner class SchedulingRequiredAppointmentToNDCreateAppointment {

    @Test
    fun `map all fields`() {
      val result = SchedulingRequiredAppointment(
        date = LocalDate.of(2021, 9, 8),
        startTime = LocalTime.of(1, 2),
        endTime = LocalTime.of(11, 12),
        project = SchedulingProject.valid().copy(
          code = "P1",
          providerCode = "PC1",
          teamCode = "TC1",
          projectTypeCode = "PT1",
        ),
        allocation = SchedulingAllocation.valid().copy(
          id = 55L,
        ),
      ).toNDCreateAppointment()

      assertThat(result.date).isEqualTo(LocalDate.of(2021, 9, 8))
      assertThat(result.startTime).isEqualTo(LocalTime.of(1, 2))
      assertThat(result.endTime).isEqualTo(LocalTime.of(11, 12))
      assertThat(result.providerCode.code).isEqualTo("PC1")
      assertThat(result.teamCode.code).isEqualTo("TC1")
      assertThat(result.projectCode.code).isEqualTo("P1")
      assertThat(result.projectTypeCode.code).isEqualTo("PT1")
      assertThat(result.allocationId).isEqualTo(55)
      assertThat(result.outcome).isNull()
      assertThat(result.supervisor).isNull()
      assertThat(result.notes).isNull()
      assertThat(result.hiVisWorn).isNull()
      assertThat(result.workedIntensively).isNull()
      assertThat(result.penaltyMinutes).isNull()
      assertThat(result.workQuality).isNull()
      assertThat(result.behaviour).isNull()
      assertThat(result.sensitive).isNull()
      assertThat(result.alertActive).isNull()
    }
  }
}
