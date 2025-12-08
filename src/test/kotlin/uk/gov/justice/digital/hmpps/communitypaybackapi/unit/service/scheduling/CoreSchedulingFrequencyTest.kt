package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.scheduling

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.scheduling.CoreSchedulingAsserter.SchedulingAsserterInput
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.Duration

/**
 * These Scenarios test the Allocation Frequencies are correctly applied
 */
class CoreSchedulingFrequencyTest {

  val coreSchedulingAsserter = CoreSchedulingAsserter(
    listOf(
      SchedulingProject(code = "PROJ1"),
      SchedulingProject(code = "PROJ2"),
    ),
  )

  /**
   * Once has some surprising/inconsistent behaviour. These are modelled by the ‘inconsistent behaviour’ scenarios
   */
  @Nested
  inner class Once {

    @Test
    fun `FREQ-ONCE-01 Schedule 'Once' Allocation for today`() {
      coreSchedulingAsserter.assertExistingAppointmentsInsufficient(
        input = SchedulingAsserterInput(
          dayOfWeek = MONDAY,
          requirementLength = Duration.ofHours(8),
          allocations = listOf(
            "ALLOC1-PROJ1-ONCE-MON-12:00-20:00, Starting Today",
          ),
          existingAppointments = emptyList(),
        ),
        expectedActions = listOf(
          "Create, Today, ALLOC1, 12:00-20:00",
        ),
      )
    }

    @Test
    fun `FREQ-ONCE-02 Schedule 'Once' Allocation tomorrow`() {
      coreSchedulingAsserter.assertExistingAppointmentsInsufficient(
        input = SchedulingAsserterInput(
          dayOfWeek = MONDAY,
          requirementLength = Duration.ofHours(8),
          allocations = listOf(
            "ALLOC1-PROJ1-ONCE-TUE-12:00-20:00, Starting Today",
          ),
          existingAppointments = emptyList(),
        ),
        expectedActions = listOf(
          "Create, Today+1, ALLOC1, 12:00-20:00",
        ),
      )
    }

    @Test
    fun `FREQ-ONCE-03 Schedule 'Once' Allocation in far future`() {
      coreSchedulingAsserter.assertExistingAppointmentsInsufficient(
        input = SchedulingAsserterInput(
          dayOfWeek = MONDAY,
          requirementLength = Duration.ofHours(8),
          allocations = listOf(
            "ALLOC1-PROJ1-ONCE-MON-12:00-20:00, Starting Today+700",
          ),
          existingAppointments = emptyList(),
        ),
        expectedActions = listOf(
          "Create, Today+700, ALLOC1, 12:00-20:00",
        ),
      )
    }

    @Test
    fun `FREQ-ONCE-04 Schedule 'Once' Allocation once`() {
      coreSchedulingAsserter.assertExistingAppointmentsInsufficient(
        input = SchedulingAsserterInput(
          dayOfWeek = THURSDAY,
          requirementLength = Duration.ofHours(16),
          allocations = listOf(
            "ALLOC1-PROJ1-ONCE-FRI-12:00-20:00, Starting Today+1",
          ),
          existingAppointments = emptyList(),
        ),
        expectedActions = listOf(
          "Create, Today+1, ALLOC1, 12:00-20:00",
        ),
      )
    }
  }

  @Nested
  inner class Weekly {

    @CsvSource(
      value = [
        "'ALLOC1-PROJ1-WK-MON-10:00-14:00','Create, Today, ALLOC1, 10:00-14:00'",
        "'ALLOC1-PROJ1-WK-TUE-10:00-14:00','Create, Today+1, ALLOC1, 10:00-14:00'",
        "'ALLOC1-PROJ1-WK-WED-10:00-14:00','Create, Today+2, ALLOC1, 10:00-14:00'",
        "'ALLOC1-PROJ1-WK-THU-10:00-14:00','Create, Today+3, ALLOC1, 10:00-14:00'",
        "'ALLOC1-PROJ1-WK-FRI-10:00-14:00','Create, Today+4, ALLOC1, 10:00-14:00'",
        "'ALLOC1-PROJ1-WK-SAT-10:00-14:00','Create, Today+5, ALLOC1, 10:00-14:00'",
        "'ALLOC1-PROJ1-WK-SUN-10:00-14:00','Create, Today+6, ALLOC1, 10:00-14:00'",
      ],
    )
    @ParameterizedTest
    fun `FREQ-WK-01 Schedule Weekly Allocation on the Correct Day`(
      allocation: String,
      expectedAppointment: String,
    ) {
      coreSchedulingAsserter.assertExistingAppointmentsInsufficient(
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
      coreSchedulingAsserter.assertExistingAppointmentsInsufficient(
        input = SchedulingAsserterInput(
          dayOfWeek = TUESDAY,
          requirementLength = Duration.ofHours(27),
          allocations = listOf("ALLOC1-PROJ1-WK-MON-12:00-16:30"),
          existingAppointments = emptyList(),
        ),
        expectedActions = listOf(
          "Create, Today+6, ALLOC1, 12:00-16:30",
          "Create, Today+13, ALLOC1, 12:00-16:30",
          "Create, Today+20, ALLOC1, 12:00-16:30",
          "Create, Today+27, ALLOC1, 12:00-16:30",
          "Create, Today+34, ALLOC1, 12:00-16:30",
          "Create, Today+41, ALLOC1, 12:00-16:30",
        ),
      )
    }

    @Test
    fun `FREQ-WK-03 Schedule Weekly Allocation until requirement met, Allocation starting today`() {
      coreSchedulingAsserter.assertExistingAppointmentsInsufficient(
        input = SchedulingAsserterInput(
          dayOfWeek = TUESDAY,
          requirementLength = Duration.ofHours(50),
          allocations = listOf("ALLOC1-PROJ1-WK-TUE-12:00-22:00"),
          existingAppointments = emptyList(),
        ),
        expectedActions = listOf(
          "Create, Today, ALLOC1, 12:00-22:00",
          "Create, Today+7, ALLOC1, 12:00-22:00",
          "Create, Today+14, ALLOC1, 12:00-22:00",
          "Create, Today+21, ALLOC1, 12:00-22:00",
          "Create, Today+28, ALLOC1, 12:00-22:00",
        ),
      )
    }

    @Test
    fun `FREQ-WK-04 Schedule Weekly Allocation until requirement met, Allocation starting tomorrow`() {
      coreSchedulingAsserter.assertExistingAppointmentsInsufficient(
        input = SchedulingAsserterInput(
          dayOfWeek = MONDAY,
          requirementLength = Duration.ofHours(80),
          allocations = listOf("ALLOC1-PROJ1-WK-TUE-00:00-10:00"),
          existingAppointments = emptyList(),
        ),
        expectedActions = listOf(
          "Create, Today+1, ALLOC1, 00:00-10:00",
          "Create, Today+8, ALLOC1, 00:00-10:00",
          "Create, Today+15, ALLOC1, 00:00-10:00",
          "Create, Today+22, ALLOC1, 00:00-10:00",
          "Create, Today+29, ALLOC1, 00:00-10:00",
          "Create, Today+36, ALLOC1, 00:00-10:00",
          "Create, Today+43, ALLOC1, 00:00-10:00",
          "Create, Today+50, ALLOC1, 00:00-10:00",
        ),
      )
    }
  }

  @Nested
  inner class Fortnightly {

    @CsvSource(
      value = [
        "'ALLOC1-PROJ1-FN-MON-10:00-14:00, Starting Today','Create, Today, ALLOC1, 10:00-14:00','Create, Today+14, ALLOC1, 10:00-14:00'",
        "'ALLOC1-PROJ1-FN-TUE-10:00-14:00, Starting Today','Create, Today+1, ALLOC1, 10:00-14:00','Create, Today+15, ALLOC1, 10:00-14:00'",
        "'ALLOC1-PROJ1-FN-WED-10:00-14:00, Starting Today','Create, Today+2, ALLOC1, 10:00-14:00','Create, Today+16, ALLOC1, 10:00-14:00'",
        "'ALLOC1-PROJ1-FN-THU-10:00-14:00, Starting Today','Create, Today+3, ALLOC1, 10:00-14:00','Create, Today+17, ALLOC1, 10:00-14:00'",
        "'ALLOC1-PROJ1-FN-FRI-10:00-14:00, Starting Today','Create, Today+4, ALLOC1, 10:00-14:00','Create, Today+18, ALLOC1, 10:00-14:00'",
        "'ALLOC1-PROJ1-FN-SAT-10:00-14:00, Starting Today','Create, Today+5, ALLOC1, 10:00-14:00','Create, Today+19, ALLOC1, 10:00-14:00'",
        "'ALLOC1-PROJ1-FN-SUN-10:00-14:00, Starting Today','Create, Today+6, ALLOC1, 10:00-14:00','Create, Today+20, ALLOC1, 10:00-14:00'",
      ],
    )
    @ParameterizedTest
    fun `FREQ-FN-01 Schedule Weekly Allocation on the Correct Day`(
      allocation: String,
      expectedAppointment1: String,
      expectedAppointment2: String,
    ) {
      coreSchedulingAsserter.assertExistingAppointmentsInsufficient(
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
      coreSchedulingAsserter.assertExistingAppointmentsInsufficient(
        input = SchedulingAsserterInput(
          dayOfWeek = MONDAY,
          requirementLength = Duration.ofHours(8),
          allocations = listOf("ALLOC1-PROJ1-FN-SUN-02:00-04:00, Starting Today-705"),
          existingAppointments = emptyList(),
        ),
        expectedActions = listOf(
          "Create, Today+13, ALLOC1, 02:00-04:00",
          "Create, Today+27, ALLOC1, 02:00-04:00",
          "Create, Today+41, ALLOC1, 02:00-04:00",
          "Create, Today+55, ALLOC1, 02:00-04:00",
        ),
      )
    }

    @Test
    fun `FREQ-FN-03 Schedule Fortnightly Allocation until requirement met, starting 14 days ago`() {
      coreSchedulingAsserter.assertExistingAppointmentsInsufficient(
        input = SchedulingAsserterInput(
          dayOfWeek = FRIDAY,
          requirementLength = Duration.ofHours(8),
          allocations = listOf("ALLOC1-PROJ1-FN-FRI-20:30-22:30, Starting Today-14"),
          existingAppointments = emptyList(),
        ),
        expectedActions = listOf(
          "Create, Today, ALLOC1, 20:30-22:30",
          "Create, Today+14, ALLOC1, 20:30-22:30",
          "Create, Today+28, ALLOC1, 20:30-22:30",
          "Create, Today+42, ALLOC1, 20:30-22:30",
        ),
      )
    }

    @Test
    fun `FREQ-FN-04 Schedule Fortnightly Allocation until requirement met, starting 8 days ago`() {
      coreSchedulingAsserter.assertExistingAppointmentsInsufficient(
        input = SchedulingAsserterInput(
          dayOfWeek = SATURDAY,
          requirementLength = Duration.ofHours(8),
          allocations = listOf("ALLOC1-PROJ1-FN-FRI-00:00-02:00, Starting Today-8"),
          existingAppointments = emptyList(),
        ),
        expectedActions = listOf(
          "Create, Today+6, ALLOC1, 00:00-02:00",
          "Create, Today+20, ALLOC1, 00:00-02:00",
          "Create, Today+34, ALLOC1, 00:00-02:00",
          "Create, Today+48, ALLOC1, 00:00-02:00",
        ),
      )
    }

    @Test
    fun `FREQ-FN-05 Schedule Fortnightly Allocation until requirement met, starting yesterday`() {
      coreSchedulingAsserter.assertExistingAppointmentsInsufficient(
        input = SchedulingAsserterInput(
          dayOfWeek = FRIDAY,
          requirementLength = Duration.ofHours(8),
          allocations = listOf("ALLOC1-PROJ1-FN-THU-11:00-13:00, Starting Today-1"),
          existingAppointments = emptyList(),
        ),
        expectedActions = listOf(
          "Create, Today+13, ALLOC1, 11:00-13:00",
          "Create, Today+27, ALLOC1, 11:00-13:00",
          "Create, Today+41, ALLOC1, 11:00-13:00",
          "Create, Today+55, ALLOC1, 11:00-13:00",
        ),
      )
    }

    @Test
    fun `FREQ-FN-06 Schedule Fortnightly Allocation until requirement met, starting today`() {
      coreSchedulingAsserter.assertExistingAppointmentsInsufficient(
        input = SchedulingAsserterInput(
          dayOfWeek = FRIDAY,
          requirementLength = Duration.ofHours(8),
          allocations = listOf("ALLOC1-PROJ1-FN-FRI-12:00-14:00, Starting Today"),
          existingAppointments = emptyList(),
        ),
        expectedActions = listOf(
          "Create, Today, ALLOC1, 12:00-14:00",
          "Create, Today+14, ALLOC1, 12:00-14:00",
          "Create, Today+28, ALLOC1, 12:00-14:00",
          "Create, Today+42, ALLOC1, 12:00-14:00",
        ),
      )
    }

    @Test
    fun `FREQ-FN-07 Schedule Fortnightly Allocation until requirement met, starting tomorrow`() {
      coreSchedulingAsserter.assertExistingAppointmentsInsufficient(
        input = SchedulingAsserterInput(
          dayOfWeek = SATURDAY,
          requirementLength = Duration.ofHours(8),
          allocations = listOf("ALLOC1-PROJ1-FN-SUN-13:00-15:00, Starting Today+1"),
          existingAppointments = emptyList(),
        ),
        expectedActions = listOf(
          "Create, Today+1, ALLOC1, 13:00-15:00",
          "Create, Today+15, ALLOC1, 13:00-15:00",
          "Create, Today+29, ALLOC1, 13:00-15:00",
          "Create, Today+43, ALLOC1, 13:00-15:00",
        ),
      )
    }

    @Test
    fun `FREQ-FN-08 Schedule Fortnightly Allocation until requirement met, over a year`() {
      coreSchedulingAsserter.assertExistingAppointmentsInsufficient(
        input = SchedulingAsserterInput(
          dayOfWeek = FRIDAY,
          requirementLength = Duration.ofHours(52),
          allocations = listOf("ALLOC1-PROJ1-FN-FRI-14:00-15:00, Starting Today"),
          existingAppointments = emptyList(),
        ),
        expectedActions = listOf(
          "Create, Today, ALLOC1, 14:00-15:00",
        ) + 1.rangeTo(51).map { "Create, Today+${it * 14}, ALLOC1, 14:00-15:00" },
      )
    }
  }

  @Nested
  inner class Mixed {

    @Test
    fun `FREQ-MISC-01 Multiple Allocations of different Frequencies`() {
      coreSchedulingAsserter.assertExistingAppointmentsInsufficient(
        input = SchedulingAsserterInput(
          dayOfWeek = FRIDAY,
          requirementLength = Duration.ofHours(37),
          allocations = listOf(
            "ALLOC1-PROJ1-WK-FRI-10:00-12:00, Starting Today",
            "ALLOC2-PROJ1-FN-SAT-10:00-13:00, Starting Today",
            "ALLOC3-PROJ1-WK-SUN-10:00-14:00, Starting Today",
            "ALLOC4-PROJ1-ONCE-MON-10:00-15:00, Starting Today+3",
            "ALLOC5-PROJ1-ONCE-THU-10:00-16:00, Starting Today+13",
            "ALLOC6-PROJ1-ONCE-WED-10:00-17:00, Starting Today+22",
          ),
          existingAppointments = emptyList(),
        ),
        expectedActions = listOf(
          "Create, Today, ALLOC1, 10:00-12:00",
          "Create, Today+1, ALLOC2, 10:00-13:00",
          "Create, Today+2, ALLOC3, 10:00-14:00",
          "Create, Today+3, ALLOC4, 10:00-15:00",
          "Create, Today+7, ALLOC1, 10:00-12:00",
          "Create, Today+9, ALLOC3, 10:00-14:00",
          "Create, Today+13, ALLOC5, 10:00-16:00",
          "Create, Today+14, ALLOC1, 10:00-12:00",
          "Create, Today+15, ALLOC2, 10:00-13:00",
          "Create, Today+16, ALLOC3, 10:00-14:00",
          "Create, Today+21, ALLOC1, 10:00-12:00",
        ),
      )
    }

    @SchedulingNDeliusDataModelsRequired
    @SuppressWarnings("EmptyFunctionBlock")
    fun `FREQ-MISC-02 Use largest frequency defined between Allocation and linked Availability`() { }
  }
}
