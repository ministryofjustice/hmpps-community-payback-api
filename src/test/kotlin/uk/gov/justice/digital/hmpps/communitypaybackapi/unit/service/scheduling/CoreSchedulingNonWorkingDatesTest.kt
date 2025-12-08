package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.scheduling

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.scheduling.CoreSchedulingAsserter.SchedulingAsserterInput
import java.time.DayOfWeek.MONDAY
import java.time.Duration

class CoreSchedulingNonWorkingDatesTest {

  val coreSchedulingAsserter = CoreSchedulingAsserter(
    listOf(
      SchedulingProject(code = "PROJ1"),
      SchedulingProject(code = "PROJ2"),
    ),
  )

  @Test
  fun `DATES-NWD-01 Once Frequency Ignored if Non Working Day`() {
    coreSchedulingAsserter.assertExistingAppointmentsInsufficient(
      input = SchedulingAsserterInput(
        dayOfWeek = MONDAY,
        requirementLength = Duration.ofHours(8),
        allocations = listOf(
          "ALLOC1-PROJ1-ONCE-SAT-10:00-18:00, Starting Today, Ending Today+7",
        ),
        existingAppointments = emptyList(),
        nonWorkingDates = listOf("TODAY+5"),
      ),
      expectedActions = emptyList(),
    )
  }

  @Test
  fun `DATES-NWD-02 Week Frequency Skips Non Working Day`() {
    coreSchedulingAsserter.assertExistingAppointmentsInsufficient(
      input = SchedulingAsserterInput(
        dayOfWeek = MONDAY,
        requirementLength = Duration.ofHours(32),
        allocations = listOf(
          "ALLOC1-PROJ1-WK-MON-10:00-18:00",
        ),
        existingAppointments = emptyList(),
        nonWorkingDates = listOf("TODAY+7"),
      ),
      expectedActions = listOf(
        "Create, Today, ALLOC1, 10:00-18:00",
        "Create, Today+14, ALLOC1, 10:00-18:00",
        "Create, Today+21, ALLOC1, 10:00-18:00",
        "Create, Today+28, ALLOC1, 10:00-18:00",
      ),
    )
  }

  @Test
  fun `DATES-NWD-03 Fortnightly frequency skips non working day`() {
    coreSchedulingAsserter.assertExistingAppointmentsInsufficient(
      input = SchedulingAsserterInput(
        dayOfWeek = MONDAY,
        requirementLength = Duration.ofHours(32),
        allocations = listOf(
          "ALLOC1-PROJ1-FN-MON-10:00-18:00, Starting Today",
        ),
        existingAppointments = emptyList(),
        nonWorkingDates = listOf("TODAY+14"),
      ),
      expectedActions = listOf(
        "Create, Today, ALLOC1, 10:00-18:00",
        "Create, Today+28, ALLOC1, 10:00-18:00",
        "Create, Today+42, ALLOC1, 10:00-18:00",
        "Create, Today+56, ALLOC1, 10:00-18:00",
      ),
    )
  }
}
