package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.scheduling.scenarios

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.DayOfWeek.MONDAY
import java.time.Duration

/**
 * These scenarios highlight inconsistent behaviour that we've emulated
 * from the NDelius implementation
 */
class SchedulingScenariosInconsistentBehavioursTest : SchedulingScenariosUnitTest() {

  @Nested
  inner class AllocationClashesDoubleBookings {

    @Test
    fun `INC-CLASH-01 Double Bookings are made if double booked allocations exist and there are no existing appointments on that date`() {
      assertExistingAppointmentsInsufficient(
        input = SchedulingScenarioAsserter.SchedulingAsserterInput(
          dayOfWeek = MONDAY,
          requirementLength = Duration.ofHours(44),
          allocations = listOf(
            "ALLOC1-PROJ1-ONCE-MON-10:00-14:00, Starting Today+7, Ending Today+7",
            "ALLOC2-PROJ2-WK-MON-12:00-20:00",
            "ALLOC3-PROJ3-WK-MON-10:00-18:00",
            "ALLOC4-PROJ4-FN-MON-06:00-14:00, Starting Today-7",
          ),
          existingAppointments = emptyList(),
        ),
        expectedActions = listOf(
          "Create, Today, PROJ2, ALLOC2, 12:00-20:00",
          "Create, Today, PROJ3, ALLOC3, 10:00-18:00",
          "Create, Today+7, PROJ1, ALLOC1, 10:00-14:00",
          "Create, Today+7, PROJ2, ALLOC2, 12:00-20:00",
          "Create, Today+7, PROJ3, ALLOC3, 10:00-18:00",
          "Create, Today+7, PROJ4, ALLOC4, 06:00-14:00",
        ),
      )
    }

    @Test
    fun `INC-CLASH-02 Double Bookings are not made if double booked allocations exist and there is at least one appointment on the date already, has outcome`() {
      assertExistingAppointmentsInsufficient(
        input = SchedulingScenarioAsserter.SchedulingAsserterInput(
          dayOfWeek = MONDAY,
          requirementLength = Duration.ofHours(58),
          allocations = listOf(
            "ALLOC1-PROJ1-ONCE-MON-10:00-14:00, Starting Today+7, Ending Today+7",
            "ALLOC2-PROJ2-WK-MON-12:00-20:00",
            "ALLOC3-PROJ3-WK-MON-10:00-18:00",
          ),
          existingAppointments = listOf(
            "Today, PROJ4, MANUAL, 12:00-20:00, Credited PT6H",
          ),
        ),
        expectedActions = listOf(
          "Create, Today+7, PROJ1, ALLOC1, 10:00-14:00",
          "Create, Today+7, PROJ2, ALLOC2, 12:00-20:00",
          "Create, Today+7, PROJ3, ALLOC3, 10:00-18:00",
          "Create, Today+14, PROJ2, ALLOC2, 12:00-20:00",
          "Create, Today+14, PROJ3, ALLOC3, 10:00-18:00",
          "Create, Today+21, PROJ2, ALLOC2, 12:00-20:00",
          "Create, Today+21, PROJ3, ALLOC3, 10:00-18:00",
        ),
      )
    }

    @Test
    fun `INC-CLASH-03 Double Bookings are not made if double booked allocations exist and there is at least one appointment on the date already, pending`() {
      assertExistingAppointmentsInsufficient(
        input = SchedulingScenarioAsserter.SchedulingAsserterInput(
          dayOfWeek = MONDAY,
          requirementLength = Duration.ofHours(30),
          allocations = listOf(
            "ALLOC1-PROJ1-ONCE-MON-10:00-14:00, Starting Today+7, Ending Today+7",
            "ALLOC2-PROJ2-WK-MON-12:00-20:00",
          ),
          existingAppointments = listOf(
            "Today, PROJ4, MANUAL, 12:00-20:00, Pending",
          ),
        ),
        expectedActions = listOf(
          "Create, Today+7, PROJ1, ALLOC1, 10:00-14:00",
          "Create, Today+7, PROJ2, ALLOC2, 12:00-20:00",
          "Create, Today+14, PROJ2, ALLOC2, 12:00-20:00",
          "Create, Today+21, PROJ2, ALLOC2, 12:00-14:00",
        ),
      )
    }
  }

  @Nested
  inner class ManualAppointmentsAndScheduling {

    @Test
    fun `INC-MANUAL-01 Manually created appointments in the future without an outcome are retained by the scheduler if attempting to allocate to same day`() {
      assertExistingAppointmentsInsufficient(
        input = SchedulingScenarioAsserter.SchedulingAsserterInput(
          dayOfWeek = MONDAY,
          requirementLength = Duration.ofHours(24),
          allocations = listOf(
            "ALLOC1-PROJ1-WK-MON-12:00-20:00",
          ),
          existingAppointments = listOf(
            "Today, PROJ1, MANUAL, 12:00-20:00, Credited PT8H",
            "Today+7, PROJ1, MANUAL, 12:00-13:00, Pending",
          ),
        ),
        expectedActions = listOf(
          "Create, Today+14, PROJ1, ALLOC1, 12:00-20:00",
          "Create, Today+21, PROJ1, ALLOC1, 12:00-19:00",
        ),
      )
    }

    @Test
    fun `INC-MANUAL-02 Appointments in the future are retained but potential time credited ignored if not attempting to allocate to same day`() {
      assertExistingAppointmentsInsufficient(
        input = SchedulingScenarioAsserter.SchedulingAsserterInput(
          dayOfWeek = MONDAY,
          requirementLength = Duration.ofHours(16),
          allocations = listOf(
            "ALLOC1-PROJ1-WK-MON-12:00-20:00",
          ),
          existingAppointments = listOf(
            "Today, PROJ1, MANUAL, 12:00-20:00, Credited PT8H",
            "Today+1, PROJ1, MANUAL, 00:00-23:00, Pending",
          ),
        ),
        expectedActions = listOf(
          "Create, Today+7, PROJ1, ALLOC1, 12:00-20:00",
        ),
      )
    }
  }
}
