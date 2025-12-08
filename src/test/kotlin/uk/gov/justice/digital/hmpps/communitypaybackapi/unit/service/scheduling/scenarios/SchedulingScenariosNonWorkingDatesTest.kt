package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.scheduling.scenarios

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.scheduling.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingProject
import java.time.DayOfWeek.MONDAY
import java.time.Duration

class SchedulingScenariosNonWorkingDatesTest : SchedulingScenariosUnitTest() {

  val schedulingAsserter = SchedulingScenarioAsserter(
    listOf(
      SchedulingProject.valid().copy(code = "PROJ1"),
      SchedulingProject.valid().copy(code = "PROJ2"),
    ),
  )

  @Test
  fun `DATES-NWD-01 Once Frequency Ignored if Non Working Day`() {
    assertExistingAppointmentsInsufficient(
      input = SchedulingScenarioAsserter.SchedulingAsserterInput(
        dayOfWeek = MONDAY,
        requirementLength = Duration.ofHours(8),
        allocations = listOf(
          "ALLOC1-PROJ1-ONCE-SAT-10:00-18:00, Starting Today, Ending Today+7",
        ),
        existingAppointments = emptyList(),
        nonWorkingDates = listOf("TODAY+5"),
      ),
      expectedShortfall = Duration.ofHours(8),
      expectedActions = emptyList(),
    )
  }

  @Test
  fun `DATES-NWD-02 Week Frequency Skips Non Working Days`() {
    assertExistingAppointmentsInsufficient(
      input = SchedulingScenarioAsserter.SchedulingAsserterInput(
        dayOfWeek = MONDAY,
        requirementLength = Duration.ofHours(32),
        allocations = listOf(
          "ALLOC1-PROJ1-WK-MON-10:00-18:00",
        ),
        existingAppointments = emptyList(),
        nonWorkingDates = listOf("TODAY+7", "TODAY+14"),
      ),
      expectedActions = listOf(
        "Create, Today, PROJ1, ALLOC1, 10:00-18:00",
        "Create, Today+21, PROJ1, ALLOC1, 10:00-18:00",
        "Create, Today+28, PROJ1, ALLOC1, 10:00-18:00",
        "Create, Today+35, PROJ1, ALLOC1, 10:00-18:00",
      ),
    )
  }

  @Test
  fun `DATES-NWD-03 Fortnightly frequency skips non working day`() {
    assertExistingAppointmentsInsufficient(
      input = SchedulingScenarioAsserter.SchedulingAsserterInput(
        dayOfWeek = MONDAY,
        requirementLength = Duration.ofHours(32),
        allocations = listOf(
          "ALLOC1-PROJ1-FN-MON-10:00-18:00, Starting Today",
        ),
        existingAppointments = emptyList(),
        nonWorkingDates = listOf("TODAY+14"),
      ),
      expectedActions = listOf(
        "Create, Today, PROJ1, ALLOC1, 10:00-18:00",
        "Create, Today+28, PROJ1, ALLOC1, 10:00-18:00",
        "Create, Today+42, PROJ1, ALLOC1, 10:00-18:00",
        "Create, Today+56, PROJ1, ALLOC1, 10:00-18:00",
      ),
    )
  }

  @Test
  fun `DATES-NWD-04 Ignore Non Working Day if it is Today`() {
    assertExistingAppointmentsInsufficient(
      input = SchedulingScenarioAsserter.SchedulingAsserterInput(
        dayOfWeek = MONDAY,
        requirementLength = Duration.ofHours(32),
        allocations = listOf(
          "ALLOC1-PROJ1-WK-MON-10:00-18:00",
        ),
        existingAppointments = emptyList(),
        nonWorkingDates = listOf("TODAY"),
      ),
      expectedActions = listOf(
        "Create, Today+7, PROJ1, ALLOC1, 10:00-18:00",
        "Create, Today+14, PROJ1, ALLOC1, 10:00-18:00",
        "Create, Today+21, PROJ1, ALLOC1, 10:00-18:00",
        "Create, Today+28, PROJ1, ALLOC1, 10:00-18:00",
      ),
    )
  }
}
