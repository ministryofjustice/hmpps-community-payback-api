package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.scheduling

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.scheduling.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.scheduling.SchedulingAsserter.SchedulingAsserterInput
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.Duration

/**
 * These Scenarios test the Allocation Frequencies are correctly applied
 */
class SchedulingFrequencyTest {

  val schedulingAsserter = SchedulingAsserter(
    listOf(
      SchedulingProject.valid().copy(code = "PROJ1"),
      SchedulingProject.valid().copy(code = "PROJ2"),
      SchedulingProject.valid().copy(code = "PROJ3"),
      SchedulingProject.valid().copy(code = "PROJ4"),
      SchedulingProject.valid().copy(code = "PROJ5"),
      SchedulingProject.valid().copy(code = "PROJ6"),
    ),
  )

  /**
   * Once has some surprising/inconsistent behaviour. These are modelled by the ‘inconsistent behaviour’ scenarios
   */
  @Nested
  inner class Once {

    @Test
    fun `FREQ-ONCE-01 Schedule 'Once' Allocation for today`() {
      schedulingAsserter.assertExistingAppointmentsInsufficient(
        input = SchedulingAsserterInput(
          dayOfWeek = MONDAY,
          requirementLength = Duration.ofHours(8),
          allocations = listOf(
            "ALLOC1-PROJ1-ONCE-MON-12:00-20:00, Starting Today",
          ),
          existingAppointments = emptyList(),
        ),
        expectedActions = listOf(
          "Create, Today, PROJ1, ALLOC1, 12:00-20:00",
        ),
      )
    }

    @Test
    fun `FREQ-ONCE-02 Schedule 'Once' Allocation tomorrow`() {
      schedulingAsserter.assertExistingAppointmentsInsufficient(
        input = SchedulingAsserterInput(
          dayOfWeek = MONDAY,
          requirementLength = Duration.ofHours(8),
          allocations = listOf(
            "ALLOC1-PROJ1-ONCE-TUE-12:00-20:00, Starting Today",
          ),
          existingAppointments = emptyList(),
        ),
        expectedActions = listOf(
          "Create, Today+1, PROJ1, ALLOC1, 12:00-20:00",
        ),
      )
    }

    @Test
    fun `FREQ-ONCE-03 Schedule 'Once' Allocation in far future`() {
      schedulingAsserter.assertExistingAppointmentsInsufficient(
        input = SchedulingAsserterInput(
          dayOfWeek = MONDAY,
          requirementLength = Duration.ofHours(8),
          allocations = listOf(
            "ALLOC1-PROJ1-ONCE-MON-12:00-20:00, Starting Today+700",
          ),
          existingAppointments = emptyList(),
        ),
        expectedActions = listOf(
          "Create, Today+700, PROJ1, ALLOC1, 12:00-20:00",
        ),
      )
    }

    @Test
    fun `FREQ-ONCE-04 Schedule 'Once' Allocation once`() {
      schedulingAsserter.assertExistingAppointmentsInsufficient(
        input = SchedulingAsserterInput(
          dayOfWeek = THURSDAY,
          requirementLength = Duration.ofHours(16),
          allocations = listOf(
            "ALLOC1-PROJ1-ONCE-FRI-12:00-20:00, Starting Today+1",
          ),
          existingAppointments = emptyList(),
        ),
        expectedShortfall = Duration.ofHours(8),
        expectedActions = listOf(
          "Create, Today+1, PROJ1, ALLOC1, 12:00-20:00",
        ),
      )
    }
  }

  @Nested
  inner class Weekly {

    @CsvSource(
      value = [
        "'ALLOC1-PROJ1-WK-MON-10:00-14:00','Create, Today, PROJ1, ALLOC1, 10:00-14:00'",
        "'ALLOC1-PROJ1-WK-TUE-10:00-14:00','Create, Today+1, PROJ1, ALLOC1, 10:00-14:00'",
        "'ALLOC1-PROJ1-WK-WED-10:00-14:00','Create, Today+2, PROJ1, ALLOC1, 10:00-14:00'",
        "'ALLOC1-PROJ1-WK-THU-10:00-14:00','Create, Today+3, PROJ1, ALLOC1, 10:00-14:00'",
        "'ALLOC1-PROJ1-WK-FRI-10:00-14:00','Create, Today+4, PROJ1, ALLOC1, 10:00-14:00'",
        "'ALLOC1-PROJ1-WK-SAT-10:00-14:00','Create, Today+5, PROJ1, ALLOC1, 10:00-14:00'",
        "'ALLOC1-PROJ1-WK-SUN-10:00-14:00','Create, Today+6, PROJ1, ALLOC1, 10:00-14:00'",
      ],
    )
    @ParameterizedTest
    fun `FREQ-WK-01 Schedule Weekly Allocation on the Correct Day`(
      allocation: String,
      expectedAppointment: String,
    ) {
      schedulingAsserter.assertExistingAppointmentsInsufficient(
        input = SchedulingAsserterInput(
          dayOfWeek = MONDAY,
          requirementLength = Duration.ofHours(4),
          allocations = listOf(allocation),
          existingAppointments = emptyList(),
        ),
        expectedActions = listOf(expectedAppointment),
      )
    }

    @Test
    fun `FREQ-WK-02 Schedule Weekly Allocation until requirement met, Allocation starting yesterday`() {
      schedulingAsserter.assertExistingAppointmentsInsufficient(
        input = SchedulingAsserterInput(
          dayOfWeek = TUESDAY,
          requirementLength = Duration.ofHours(27),
          allocations = listOf("ALLOC1-PROJ1-WK-MON-12:00-16:30"),
          existingAppointments = emptyList(),
        ),
        expectedActions = listOf(
          "Create, Today+6, PROJ1, ALLOC1, 12:00-16:30",
          "Create, Today+13, PROJ1, ALLOC1, 12:00-16:30",
          "Create, Today+20, PROJ1, ALLOC1, 12:00-16:30",
          "Create, Today+27, PROJ1, ALLOC1, 12:00-16:30",
          "Create, Today+34, PROJ1, ALLOC1, 12:00-16:30",
          "Create, Today+41, PROJ1, ALLOC1, 12:00-16:30",
        ),
      )
    }

    @Test
    fun `FREQ-WK-03 Schedule Weekly Allocation until requirement met, Allocation starting today`() {
      schedulingAsserter.assertExistingAppointmentsInsufficient(
        input = SchedulingAsserterInput(
          dayOfWeek = TUESDAY,
          requirementLength = Duration.ofHours(50),
          allocations = listOf("ALLOC1-PROJ1-WK-TUE-12:00-22:00"),
          existingAppointments = emptyList(),
        ),
        expectedActions = listOf(
          "Create, Today, PROJ1, ALLOC1, 12:00-22:00",
          "Create, Today+7, PROJ1, ALLOC1, 12:00-22:00",
          "Create, Today+14, PROJ1, ALLOC1, 12:00-22:00",
          "Create, Today+21, PROJ1, ALLOC1, 12:00-22:00",
          "Create, Today+28, PROJ1, ALLOC1, 12:00-22:00",
        ),
      )
    }

    @Test
    fun `FREQ-WK-04 Schedule Weekly Allocation until requirement met, Allocation starting tomorrow`() {
      schedulingAsserter.assertExistingAppointmentsInsufficient(
        input = SchedulingAsserterInput(
          dayOfWeek = MONDAY,
          requirementLength = Duration.ofHours(80),
          allocations = listOf("ALLOC1-PROJ1-WK-TUE-00:00-10:00"),
          existingAppointments = emptyList(),
        ),
        expectedActions = listOf(
          "Create, Today+1, PROJ1, ALLOC1, 00:00-10:00",
          "Create, Today+8, PROJ1, ALLOC1, 00:00-10:00",
          "Create, Today+15, PROJ1, ALLOC1, 00:00-10:00",
          "Create, Today+22, PROJ1, ALLOC1, 00:00-10:00",
          "Create, Today+29, PROJ1, ALLOC1, 00:00-10:00",
          "Create, Today+36, PROJ1, ALLOC1, 00:00-10:00",
          "Create, Today+43, PROJ1, ALLOC1, 00:00-10:00",
          "Create, Today+50, PROJ1, ALLOC1, 00:00-10:00",
        ),
      )
    }
  }

  @Nested
  inner class Fortnightly {

    @CsvSource(
      value = [
        "'ALLOC1-PROJ1-FN-MON-10:00-14:00, Starting Today','Create, Today, PROJ1, ALLOC1, 10:00-14:00','Create, Today+14, PROJ1, ALLOC1, 10:00-14:00'",
        "'ALLOC1-PROJ1-FN-TUE-10:00-14:00, Starting Today','Create, Today+1, PROJ1, ALLOC1, 10:00-14:00','Create, Today+15, PROJ1, ALLOC1, 10:00-14:00'",
        "'ALLOC1-PROJ1-FN-WED-10:00-14:00, Starting Today','Create, Today+2, PROJ1, ALLOC1, 10:00-14:00','Create, Today+16, PROJ1, ALLOC1, 10:00-14:00'",
        "'ALLOC1-PROJ1-FN-THU-10:00-14:00, Starting Today','Create, Today+3, PROJ1, ALLOC1, 10:00-14:00','Create, Today+17, PROJ1, ALLOC1, 10:00-14:00'",
        "'ALLOC1-PROJ1-FN-FRI-10:00-14:00, Starting Today','Create, Today+4, PROJ1, ALLOC1, 10:00-14:00','Create, Today+18, PROJ1, ALLOC1, 10:00-14:00'",
        "'ALLOC1-PROJ1-FN-SAT-10:00-14:00, Starting Today','Create, Today+5, PROJ1, ALLOC1, 10:00-14:00','Create, Today+19, PROJ1, ALLOC1, 10:00-14:00'",
        "'ALLOC1-PROJ1-FN-SUN-10:00-14:00, Starting Today','Create, Today+6, PROJ1, ALLOC1, 10:00-14:00','Create, Today+20, PROJ1, ALLOC1, 10:00-14:00'",
      ],
    )
    @ParameterizedTest
    fun `FREQ-FN-01 Schedule Weekly Allocation on the Correct Day`(
      allocation: String,
      expectedAppointment1: String,
      expectedAppointment2: String,
    ) {
      schedulingAsserter.assertExistingAppointmentsInsufficient(
        input = SchedulingAsserterInput(
          dayOfWeek = MONDAY,
          requirementLength = Duration.ofHours(8),
          allocations = listOf(allocation),
          existingAppointments = emptyList(),
        ),
        expectedActions = listOf(expectedAppointment1, expectedAppointment2),
      )
    }

    @Test
    fun `FREQ-FN-02 Schedule Fortnightly Allocation until requirement met, iterating from Allocation start date`() {
      schedulingAsserter.assertExistingAppointmentsInsufficient(
        input = SchedulingAsserterInput(
          dayOfWeek = MONDAY,
          requirementLength = Duration.ofHours(8),
          allocations = listOf("ALLOC1-PROJ1-FN-SUN-02:00-04:00, Starting Today-705"),
          existingAppointments = emptyList(),
        ),
        expectedActions = listOf(
          "Create, Today+13, PROJ1, ALLOC1, 02:00-04:00",
          "Create, Today+27, PROJ1, ALLOC1, 02:00-04:00",
          "Create, Today+41, PROJ1, ALLOC1, 02:00-04:00",
          "Create, Today+55, PROJ1, ALLOC1, 02:00-04:00",
        ),
      )
    }

    @Test
    fun `FREQ-FN-03 Schedule Fortnightly Allocation until requirement met, starting 14 days ago`() {
      schedulingAsserter.assertExistingAppointmentsInsufficient(
        input = SchedulingAsserterInput(
          dayOfWeek = FRIDAY,
          requirementLength = Duration.ofHours(8),
          allocations = listOf("ALLOC1-PROJ1-FN-FRI-20:30-22:30, Starting Today-14"),
          existingAppointments = emptyList(),
        ),
        expectedActions = listOf(
          "Create, Today, PROJ1, ALLOC1, 20:30-22:30",
          "Create, Today+14, PROJ1, ALLOC1, 20:30-22:30",
          "Create, Today+28, PROJ1, ALLOC1, 20:30-22:30",
          "Create, Today+42, PROJ1, ALLOC1, 20:30-22:30",
        ),
      )
    }

    @Test
    fun `FREQ-FN-04 Schedule Fortnightly Allocation until requirement met, starting 8 days ago`() {
      schedulingAsserter.assertExistingAppointmentsInsufficient(
        input = SchedulingAsserterInput(
          dayOfWeek = SATURDAY,
          requirementLength = Duration.ofHours(8),
          allocations = listOf("ALLOC1-PROJ1-FN-FRI-00:00-02:00, Starting Today-8"),
          existingAppointments = emptyList(),
        ),
        expectedActions = listOf(
          "Create, Today+6, PROJ1, ALLOC1, 00:00-02:00",
          "Create, Today+20, PROJ1, ALLOC1, 00:00-02:00",
          "Create, Today+34, PROJ1, ALLOC1, 00:00-02:00",
          "Create, Today+48, PROJ1, ALLOC1, 00:00-02:00",
        ),
      )
    }

    @Test
    fun `FREQ-FN-05 Schedule Fortnightly Allocation until requirement met, starting yesterday`() {
      schedulingAsserter.assertExistingAppointmentsInsufficient(
        input = SchedulingAsserterInput(
          dayOfWeek = FRIDAY,
          requirementLength = Duration.ofHours(8),
          allocations = listOf("ALLOC1-PROJ1-FN-THU-11:00-13:00, Starting Today-1"),
          existingAppointments = emptyList(),
        ),
        expectedActions = listOf(
          "Create, Today+13, PROJ1, ALLOC1, 11:00-13:00",
          "Create, Today+27, PROJ1, ALLOC1, 11:00-13:00",
          "Create, Today+41, PROJ1, ALLOC1, 11:00-13:00",
          "Create, Today+55, PROJ1, ALLOC1, 11:00-13:00",
        ),
      )
    }

    @Test
    fun `FREQ-FN-06 Schedule Fortnightly Allocation until requirement met, starting today`() {
      schedulingAsserter.assertExistingAppointmentsInsufficient(
        input = SchedulingAsserterInput(
          dayOfWeek = FRIDAY,
          requirementLength = Duration.ofHours(8),
          allocations = listOf("ALLOC1-PROJ1-FN-FRI-12:00-14:00, Starting Today"),
          existingAppointments = emptyList(),
        ),
        expectedActions = listOf(
          "Create, Today, PROJ1, ALLOC1, 12:00-14:00",
          "Create, Today+14, PROJ1, ALLOC1, 12:00-14:00",
          "Create, Today+28, PROJ1, ALLOC1, 12:00-14:00",
          "Create, Today+42, PROJ1, ALLOC1, 12:00-14:00",
        ),
      )
    }

    @Test
    fun `FREQ-FN-07 Schedule Fortnightly Allocation until requirement met, starting tomorrow`() {
      schedulingAsserter.assertExistingAppointmentsInsufficient(
        input = SchedulingAsserterInput(
          dayOfWeek = SATURDAY,
          requirementLength = Duration.ofHours(8),
          allocations = listOf("ALLOC1-PROJ1-FN-SUN-13:00-15:00, Starting Today+1"),
          existingAppointments = emptyList(),
        ),
        expectedActions = listOf(
          "Create, Today+1, PROJ1, ALLOC1, 13:00-15:00",
          "Create, Today+15, PROJ1, ALLOC1, 13:00-15:00",
          "Create, Today+29, PROJ1, ALLOC1, 13:00-15:00",
          "Create, Today+43, PROJ1, ALLOC1, 13:00-15:00",
        ),
      )
    }

    @Test
    fun `FREQ-FN-08 Schedule Fortnightly Allocation until requirement met, over a year`() {
      schedulingAsserter.assertExistingAppointmentsInsufficient(
        input = SchedulingAsserterInput(
          dayOfWeek = FRIDAY,
          requirementLength = Duration.ofHours(52),
          allocations = listOf("ALLOC1-PROJ1-FN-FRI-14:00-15:00, Starting Today"),
          existingAppointments = emptyList(),
        ),
        expectedActions = listOf(
          "Create, Today, PROJ1, ALLOC1, 14:00-15:00",
        ) + 1.rangeTo(51).map { "Create, Today+${it * 14}, PROJ1, ALLOC1, 14:00-15:00" },
      )
    }
  }

  @Nested
  inner class Mixed {

    @Test
    fun `FREQ-MIXED-01 Multiple Allocations of different Frequencies`() {
      schedulingAsserter.assertExistingAppointmentsInsufficient(
        input = SchedulingAsserterInput(
          dayOfWeek = FRIDAY,
          requirementLength = Duration.ofHours(37),
          allocations = listOf(
            "ALLOC1-PROJ1-WK-FRI-10:00-12:00, Starting Today",
            "ALLOC2-PROJ2-FN-SAT-10:00-13:00, Starting Today",
            "ALLOC3-PROJ3-WK-SUN-10:00-14:00, Starting Today",
            "ALLOC4-PROJ4-ONCE-MON-10:00-15:00, Starting Today+3",
            "ALLOC5-PROJ5-ONCE-THU-10:00-16:00, Starting Today+13",
            "ALLOC6-PROJ6-ONCE-WED-10:00-17:00, Starting Today+22",
          ),
          existingAppointments = emptyList(),
        ),
        expectedActions = listOf(
          "Create, Today, PROJ1, ALLOC1, 10:00-12:00",
          "Create, Today+1, PROJ2, ALLOC2, 10:00-13:00",
          "Create, Today+2, PROJ3, ALLOC3, 10:00-14:00",
          "Create, Today+3, PROJ4, ALLOC4, 10:00-15:00",
          "Create, Today+7, PROJ1, ALLOC1, 10:00-12:00",
          "Create, Today+9, PROJ3, ALLOC3, 10:00-14:00",
          "Create, Today+13, PROJ5, ALLOC5, 10:00-16:00",
          "Create, Today+14, PROJ1, ALLOC1, 10:00-12:00",
          "Create, Today+15, PROJ2, ALLOC2, 10:00-13:00",
          "Create, Today+16, PROJ3, ALLOC3, 10:00-14:00",
          "Create, Today+21, PROJ1, ALLOC1, 10:00-12:00",
        ),
      )
    }

    @Disabled
    fun `FREQ-MIXED-02 Use largest frequency defined between Allocation and linked Availability`() {
      // This scenario is implicitly tested by the [SchedulingMappersTest] which ensures the
      // largest frequency is used when building the internal allocation models
    }
  }
}
