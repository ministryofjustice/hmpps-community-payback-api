package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.scheduling

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.scheduling.CoreSchedulingAsserter.SchedulingAsserterInput
import java.time.DayOfWeek.MONDAY
import java.time.Duration

/**
 * These scenarios will check that the remaining time is correctly calculated by checking
 * appointments are created that are sufficient to meet this remaining time
 *
 * They will also ensure that the final appointment end time is truncated where necessary to ensure
 * no more minutes than required are scheduled
 */
class CoreSchedulingRemainingTimeTest {

  val coreSchedulingAsserter = CoreSchedulingAsserter(
    listOf(
      SchedulingProject(code = "PROJ1"),
      SchedulingProject(code = "PROJ2"),
    ),
  )

  @Test
  fun `REMAINING-TIME-01 0 Requirement`() {
    coreSchedulingAsserter.assertRequirementAlreadySatisfied(
      SchedulingAsserterInput(
        dayOfWeek = MONDAY,
        requirementLength = Duration.ZERO,
        allocations = listOf(
          "ALLOC1-PROJ1-WK-MON-10:00-18:00",
        ),
        existingAppointments = emptyList(),
      ),
    )
  }

  @Test
  fun `REMAINING-TIME-02 No Scheduled Appointments, Create 1 non-truncated Appointment Today`() {
    coreSchedulingAsserter.assertExistingAppointmentsInsufficient(
      input = SchedulingAsserterInput(
        dayOfWeek = MONDAY,
        requirementLength = Duration.ofHours(8),
        allocations = listOf(
          "ALLOC1-PROJ1-WK-MON-10:00-18:00",
          "ALLOC2-PROJ2-WK-TUE-16:00-20:00",
        ),
        existingAppointments = emptyList(),
      ),
      expectedActions = listOf(
        "Create, Today, ALLOC1, 10:00-18:00",
      ),
    )
  }

  @Test
  fun `REMAINING-TIME-03 No Scheduled Appointments, Create 1 truncated Appointment Today`() {
    coreSchedulingAsserter.assertExistingAppointmentsInsufficient(
      input = SchedulingAsserterInput(
        dayOfWeek = MONDAY,
        requirementLength = Duration.ofHours(4),
        allocations = listOf(
          "ALLOC1-PROJ1-WK-MON-10:00-18:00",
          "ALLOC2-PROJ2-WK-TUE-16:00-20:00",
        ),
        existingAppointments = emptyList(),
      ),
      expectedActions = listOf(
        "Create, Today, ALLOC1, 10:00-14:00",
      ),
    )
  }

  @Test
  fun `REMAINING-TIME-04 Pending Past Appointment Insufficient, Create 1 non-truncated Appointment Today`() {
    coreSchedulingAsserter.assertExistingAppointmentsInsufficient(
      input = SchedulingAsserterInput(
        dayOfWeek = MONDAY,
        requirementLength = Duration.ofHours(12),
        allocations = listOf(
          "ALLOC1-PROJ1-WK-MON-10:00-18:00",
          "ALLOC2-PROJ2-WK-TUE-16:00-20:00",
        ),
        existingAppointments = listOf(
          "Today-6, PROJ2, ALLOC2, 16:00-20:00, Pending",
        ),
      ),
      expectedActions = listOf(
        "Create, Today, ALLOC1, 10:00-18:00",
      ),
    )
  }

  @Test
  fun `REMAINING-TIME-05 Credited Past Appointment Insufficient, Create 1 non-truncated Appointment Today`() {
    coreSchedulingAsserter.assertExistingAppointmentsInsufficient(
      input = SchedulingAsserterInput(
        dayOfWeek = MONDAY,
        requirementLength = Duration.ofHours(12),
        allocations = listOf(
          "ALLOC1-PROJ1-WK-MON-10:00-18:00",
          "ALLOC2-PROJ2-WK-TUE-16:00-20:00",
        ),
        existingAppointments = listOf(
          "Today-6, PROJ2, ALLOC2, 16:00-20:00, Credited PT4H",
        ),
      ),
      expectedActions = listOf(
        "Create, Today, ALLOC1, 10:00-18:00",
      ),
    )
  }

  @Test
  fun `REMAINING-TIME-06 Non Attended Past Appointment Insufficient, Create 1 non-truncated Appointment Today`() {
    coreSchedulingAsserter.assertExistingAppointmentsInsufficient(
      input = SchedulingAsserterInput(
        dayOfWeek = MONDAY,
        requirementLength = Duration.ofHours(8),
        allocations = listOf(
          "ALLOC1-PROJ1-WK-MON-10:00-18:00",
          "ALLOC2-PROJ2-WK-TUE-16:00-20:00",
        ),
        existingAppointments = listOf(
          "Today-6, PROJ2, ALLOC2, 16:00-20:00, Non-attended",
        ),
      ),
      expectedActions = listOf(
        "Create, Today, ALLOC1, 10:00-18:00",
      ),
    )
  }

  @Test
  fun `REMAINING-TIME-07 Pending Past Appointment Insufficient, Create 1 truncated Appointment`() {
    coreSchedulingAsserter.assertExistingAppointmentsInsufficient(
      input = SchedulingAsserterInput(
        dayOfWeek = MONDAY,
        requirementLength = Duration.parse("PT10H30M"),
        allocations = listOf(
          "ALLOC1-PROJ1-WK-MON-10:00-18:00",
          "ALLOC2-PROJ2-WK-TUE-16:00-20:00",
        ),
        existingAppointments = listOf(
          "Today-6, PROJ2, ALLOC2, 10:00-14:00, Pending",
        ),
      ),
      expectedActions = listOf(
        "Create, Today, ALLOC1, 10:00-16:30",
      ),
    )
  }

  @Test
  fun `REMAINING-TIME-08 Credited Past Appointment Insufficient, Create 1 truncated Appointment`() {
    coreSchedulingAsserter.assertExistingAppointmentsInsufficient(
      input = SchedulingAsserterInput(
        dayOfWeek = MONDAY,
        requirementLength = Duration.parse("PT11H"),
        allocations = listOf(
          "ALLOC1-PROJ1-WK-MON-10:00-18:00",
          "ALLOC2-PROJ2-WK-TUE-16:00-20:00",
        ),
        existingAppointments = listOf(
          "Today-6, PROJ2, ALLOC2, 10:00-14:00, Credited PT4H",
        ),
      ),
      expectedActions = listOf(
        "Create, Today, ALLOC1, 10:00-17:00",
      ),
    )
  }

  @Test
  fun `REMAINING-TIME-09 Credited Past Appointments Insufficient, Create multiple Appointments including truncated final Appointment`() {
    coreSchedulingAsserter.assertExistingAppointmentsInsufficient(
      input = SchedulingAsserterInput(
        dayOfWeek = MONDAY,
        requirementLength = Duration.parse("PT44H"),
        allocations = listOf(
          "ALLOC1-PROJ1-WK-MON-10:00-18:00",
          "ALLOC2-PROJ2-WK-TUE-16:00-20:00",
        ),
        existingAppointments = listOf(
          "Today-14, PROJ1, ALLOC1, 10:00-18:00, Credited PT3H30M",
          "Today-13, PROJ2, ALLOC2, 10:00-14:00, Non-attended",
          "Today-7, PROJ1, ALLOC1, 10:00-18:00, Non-attended",
          "Today-6, PROJ2, ALLOC2, 10:00-14:00, Credited PT4H",
          "Today, PROJ1, ALLOC1, 10:00-18:00, Pending",
          "Today+1, PROJ2, ALLOC2, 16:00-20:00, Pending",
        ),
      ),
      expectedActions = listOf(
        "Create, Today+7, ALLOC1, 10:00-18:00",
        "Create, Today+8, ALLOC2, 16:00-20:00",
        "Create, Today+14, ALLOC1, 10:00-18:00",
        "Create, Today+15, ALLOC2, 16:00-20:00",
        "Create, Today+21, ALLOC1, 10:00-10:30",
      ),
    )
  }
}
