package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.scheduling.scenarios

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.scheduling.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingProject
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.Duration

/**
 * Existing appointments already satisfy requirements (No-op)
 *
 * Once deleting appointments is in scope, scenarios with a surplus should lead to appointments being deleted
 */
class SchedulingScenariosNoOpTest : SchedulingScenariosUnitTest() {

  val schedulingAsserter = SchedulingScenarioAsserter(
    listOf(
      SchedulingProject.valid().copy(code = "PROJ1"),
      SchedulingProject.valid().copy(code = "PROJ2"),
    ),
  )

  @Test
  fun `NOOP-01 Today's Pending Appointment Satisfies Requirement`() {
    assertExistingAppointmentsSufficient(
      input = SchedulingScenarioAsserter.SchedulingAsserterInput(
        dayOfWeek = MONDAY,
        requirementLength = Duration.ofHours(8),
        allocations = listOf(
          "ALLOC1-PROJ1-WK-MON-12:00-20:00",
        ),
        existingAppointments = listOf(
          "Today, PROJ1, ALLOC1, 12:00-20:00, Pending",
        ),
      ),
    )
  }

  @Test
  fun `NOOP-02 Today's Completed Appointment Time Credited Satisfies Requirement`() {
    assertRequirementAlreadySatisfied(
      input = SchedulingScenarioAsserter.SchedulingAsserterInput(
        dayOfWeek = MONDAY,
        requirementLength = Duration.parse("PT6H30M"),
        allocations = listOf(
          "ALLOC1-PROJ1-WK-MON-12:00-20:00",
        ),
        existingAppointments = listOf(
          "Today, PROJ1, ALLOC1, 12:00-20:00, Credited PT6H30M",
        ),
      ),
    )
  }

  @Test
  fun `NOOP-03 Today's Pending Appointment Exceeds Requirement`() {
    assertExistingAppointmentsSufficient(
      input = SchedulingScenarioAsserter.SchedulingAsserterInput(
        dayOfWeek = MONDAY,
        requirementLength = Duration.parse("PT6H30M"),
        allocations = listOf(
          "ALLOC1-PROJ1-WK-MON-10:00-20:00",
        ),
        existingAppointments = listOf(
          "Today, PROJ1, ALLOC1, 10:00-16:30, Pending",
        ),
      ),
    )
  }

  @Test
  fun `NOOP-04 Today's Complete Appointment Satisfies Requirement`() {
    assertRequirementAlreadySatisfied(
      input = SchedulingScenarioAsserter.SchedulingAsserterInput(
        dayOfWeek = MONDAY,
        requirementLength = Duration.parse("PT6H30M"),
        allocations = listOf(
          "ALLOC1-PROJ1-WK-MON-10:00-20:00",
        ),
        existingAppointments = listOf(
          "Today, PROJ1, ALLOC1, 10:00-18:00, Credited PT8H",
        ),
      ),
    )
  }

  @Test
  fun `NOOP-05 Yesterday's Pending Appointment Satisfies Requirement`() {
    assertRequirementAlreadySatisfied(
      input = SchedulingScenarioAsserter.SchedulingAsserterInput(
        dayOfWeek = TUESDAY,
        requirementLength = Duration.parse("PT6H30M"),
        allocations = listOf(
          "ALLOC1-PROJ1-WK-MON-10:00-18:00",
        ),
        existingAppointments = listOf(
          "Today-1, PROJ1, ALLOC1, 10:00-18:30, Pending",
        ),
      ),
    )
  }

  @Test
  fun `NOOP-06 Yesterday's Complete Appointment Satisfies Requirement`() {
    assertRequirementAlreadySatisfied(
      input = SchedulingScenarioAsserter.SchedulingAsserterInput(
        dayOfWeek = TUESDAY,
        requirementLength = Duration.parse("PT2H"),
        allocations = listOf(
          "ALLOC1-PROJ1-WK-MON-10:00-18:00",
        ),
        existingAppointments = listOf(
          "Today-1, PROJ1, ALLOC1, 10:00-18:00, Credited PT2H",
        ),
      ),
    )
  }

  @Test
  fun `NOOP-07 Multiple Complete and Pending Past Appointments Satisfy Requirement`() {
    assertRequirementAlreadySatisfied(
      input = SchedulingScenarioAsserter.SchedulingAsserterInput(
        dayOfWeek = MONDAY,
        requirementLength = Duration.parse("PT120H"),
        allocations = listOf(
          "ALLOC1-PROJ1-WK-MON-10:00-18:00",
        ),
        existingAppointments = listOf(
          "Today-7, PROJ1, ALLOC1, 10:00-18:00, Credited PT8H",
          "Today-14, PROJ1, ALLOC1, 10:00-18:00, Credited PT8H",
          "Today-21, PROJ1, ALLOC1, 10:00-18:00, Credited PT8H",
          "Today-28, PROJ1, ALLOC1, 10:00-18:00, Credited PT8H",
          "Today-35, PROJ1, ALLOC1, 10:00-18:00, Credited PT8H",
          "Today-42, PROJ1, ALLOC1, 10:00-18:00, Credited PT8H",
          "Today-49, PROJ1, ALLOC1, 10:00-18:00, Credited PT8H",
          "Today-56, PROJ1, ALLOC1, 10:00-18:00, Credited PT8H",
          "Today-63, PROJ1, ALLOC1, 10:00-18:00, Credited PT8H",
          "Today-70, PROJ1, ALLOC1, 10:00-18:00, Credited PT8H",
          "Today-77, PROJ1, ALLOC1, 10:00-18:00, Credited PT8H",
          "Today-84, PROJ1, ALLOC1, 10:00-18:00, Credited PT8H",
          "Today-91, PROJ1, ALLOC1, 10:00-18:00, Credited PT8H",
          "Today-98, PROJ1, ALLOC1, 10:00-18:00, Credited PT8H",
          "Today-105, PROJ1, ALLOC1, 10:00-18:00, Credited PT8H",
        ),
      ),
    )
  }

  @Test
  fun `NOOP-08 1 Past and 1 Future Pending Appointments Satisfy Requirement`() {
    assertExistingAppointmentsSufficient(
      input = SchedulingScenarioAsserter.SchedulingAsserterInput(
        dayOfWeek = WEDNESDAY,
        requirementLength = Duration.parse("PT8H"),
        allocations = listOf(
          "ALLOC1-PROJ1-WK-TUE-08:00-12:00",
        ),
        existingAppointments = listOf(
          "Today-1, PROJ1, ALLOC1, 08:00-12:00, Pending",
          "Today+6, PROJ1, ALLOC1, 08:00-12:00, Pending",
        ),
      ),
    )
  }

  @Test
  fun `NOOP-09 1 Past and 1 Future Complete Appointments Satisfy Requirement`() {
    assertRequirementAlreadySatisfied(
      input = SchedulingScenarioAsserter.SchedulingAsserterInput(
        dayOfWeek = WEDNESDAY,
        requirementLength = Duration.parse("PT8H"),
        allocations = listOf(
          "ALLOC1-PROJ1-WK-TUE-08:00-12:00",
        ),
        existingAppointments = listOf(
          "Today-1, PROJ1, ALLOC1, 08:00-12:00, Credited PT4H",
          "Today+6, PROJ1, ALLOC1, 08:00-12:00, Credited PT4H",
        ),
      ),
    )
  }

  @Test
  fun `NOOP-10 Many Past and Future Pending and Complete Appointments Across Multiple Allocations Satisfy Requirement`() {
    assertExistingAppointmentsSufficient(
      input = SchedulingScenarioAsserter.SchedulingAsserterInput(
        dayOfWeek = WEDNESDAY,
        requirementLength = Duration.parse("PT46H"),
        allocations = listOf(
          "ALLOC1-PROJ1-WK-MON-12:00-20:00",
          "ALLOC2-PROJ2-WK-TUE-10:00-14:00",
        ),
        existingAppointments = listOf(
          "Today-50, PROJ1, MANUAL, 02:00-06:00, Credited PT4H",
          "Today-2, PROJ1, ALLOC1, 12:00-18:00, Credited PT6H",
          "Today-1, PROJ2, ALLOC2, 10:00-14:00, Credited PT0H",
          "Today+5, PROJ1, ALLOC1, 12:00-20:00, Pending",
          "Today+6, PROJ2, ALLOC2, 10:00-14:00, Pending",
          "Today+12, PROJ1, ALLOC1, 12:00-20:00, Pending",
          "Today+13, PROJ2, ALLOC2, 10:00-14:00, Pending",
          "Today+19, PROJ1, ALLOC1, 12:00-20:00, Pending",
          "Today+20, PROJ2, ALLOC2, 10:00-14:00, Pending",
        ),
      ),
    )
  }

  @Test
  fun `NOOP-11 Surplus Appointments are not removed`() {
    assertRequirementAlreadySatisfied(
      input = SchedulingScenarioAsserter.SchedulingAsserterInput(
        dayOfWeek = WEDNESDAY,
        requirementLength = Duration.parse("PT8H"),
        allocations = listOf(
          "ALLOC1-PROJ1-WK-MON-12:00-20:00",
          "ALLOC2-PROJ2-WK-TUE-10:00-14:00",
        ),
        existingAppointments = listOf(
          "Today-20, PROJ1, MANUAL, 00:00-04:00, Credited PT4H",
          "Today-10, PROJ1, MANUAL, 00:30-04:30, Credited PT4H",
          "Today, PROJ1, MANUAL, 01:00-09:00, Pending",
          "Today+10, PROJ1, MANUAL, 01:30-09:30, Pending",
          "Today+20, PROJ1, MANUAL, 02:00-10:00, Pending",
        ),
      ),
    )
  }
}
