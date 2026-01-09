package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.scheduling.scenarios

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.scheduling.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingProject
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.TUESDAY
import java.time.Duration

/**
 * Ensure allocations are ignored or not ignored based upon end
 * and start dates on Allocation, Availability and Project
 */
class SchedulingScenariosStartEndDatesTest : SchedulingScenariosUnitTest() {

  val schedulingAsserter = SchedulingScenarioAsserter(
    listOf(
      SchedulingProject.valid().copy(code = "PROJ1"),
      SchedulingProject.valid().copy(code = "PROJ2"),
    ),
  )

  @Nested
  inner class StartDate {

    @Test
    fun `DATES-START-01 Allocation Start Date is tomorrow`() {
      assertExistingAppointmentsInsufficient(
        input = SchedulingScenarioAsserter.SchedulingAsserterInput(
          dayOfWeek = MONDAY,
          requirementLength = Duration.ofHours(16),
          allocations = listOf(
            "ALLOC1-PROJ1-WK-TUE-10:00-18:00. Starting Today+1",
          ),
          existingAppointments = emptyList(),
        ),
        expectedActions = listOf(
          "Create, Today+1, PROJ1, ALLOC1, 10:00-18:00",
          "Create, Today+8, PROJ1, ALLOC1, 10:00-18:00",
        ),
      )
    }

    @Test
    fun `DATES-START-02 Allocation Start Date is today`() {
      assertExistingAppointmentsInsufficient(
        input = SchedulingScenarioAsserter.SchedulingAsserterInput(
          dayOfWeek = TUESDAY,
          requirementLength = Duration.ofHours(16),
          allocations = listOf(
            "ALLOC1-PROJ1-WK-TUE-10:00-18:00. Starting Today",
          ),
          existingAppointments = emptyList(),
        ),
        expectedActions = listOf(
          "Create, Today, PROJ1, ALLOC1, 10:00-18:00",
          "Create, Today+7, PROJ1, ALLOC1, 10:00-18:00",
        ),
      )
    }

    @Test
    fun `DATES-START-03 Allocation Start Date is in far future`() {
      assertExistingAppointmentsInsufficient(
        input = SchedulingScenarioAsserter.SchedulingAsserterInput(
          dayOfWeek = TUESDAY,
          requirementLength = Duration.ofHours(16),
          allocations = listOf(
            "ALLOC1-PROJ1-WK-TUE-10:00-18:00. Starting Today+700",
          ),
          existingAppointments = emptyList(),
        ),
        expectedActions = listOf(
          "Create, Today+700, PROJ1, ALLOC1, 10:00-18:00",
          "Create, Today+707, PROJ1, ALLOC1, 10:00-18:00",
        ),
      )
    }
  }

  @Nested
  inner class EndDates {

    @Test
    fun `DATES-END-01 Allocation End Date is day of next iteration`() {
      assertExistingAppointmentsInsufficient(
        input = SchedulingScenarioAsserter.SchedulingAsserterInput(
          dayOfWeek = MONDAY,
          requirementLength = Duration.ofHours(80),
          allocations = listOf(
            "ALLOC1-PROJ1-FN-MON-10:00-18:00, Starting Today, Ending Today+14",
          ),
          existingAppointments = emptyList(),
        ),
        expectedShortfall = Duration.ofHours(64),
        expectedActions = listOf(
          "Create, Today, PROJ1, ALLOC1, 10:00-18:00",
          "Create, Today+14, PROJ1, ALLOC1, 10:00-18:00",
        ),
      )
    }

    @Test
    fun `DATES-END-02 Allocation End Date is day before next iteration`() {
      assertExistingAppointmentsInsufficient(
        input = SchedulingScenarioAsserter.SchedulingAsserterInput(
          dayOfWeek = MONDAY,
          requirementLength = Duration.ofHours(80),
          allocations = listOf(
            "ALLOC1-PROJ1-FN-MON-10:00-18:00, Starting Today, Ending Today+13",
          ),
          existingAppointments = emptyList(),
        ),
        expectedShortfall = Duration.ofHours(72),
        expectedActions = listOf(
          "Create, Today, PROJ1, ALLOC1, 10:00-18:00",
        ),
      )
    }

    @Disabled
    fun `DATES-END-03 Allocation Start Date is same as End Date`() {
      // This scenario is implicitly tested by the [SchedulingMappersTest] which will ensure
      // these allocations are filtered out when mapping the data models
    }

    @Test
    fun `DATES-END-04 Allocation End Date is so close to Start Date it prohibits Appointments being created`() {
      assertExistingAppointmentsInsufficient(
        input = SchedulingScenarioAsserter.SchedulingAsserterInput(
          dayOfWeek = TUESDAY,
          requirementLength = Duration.ofHours(80),
          allocations = listOf(
            "ALLOC1-PROJ1-FN-MON-10:00-18:00. Starting Today-1, Ending Today+12",
          ),
          existingAppointments = emptyList(),
        ),
        expectedShortfall = Duration.ofHours(80),
        expectedActions = emptyList(),
      )
    }

    @Test
    fun `DATES-END-05 Allocation ends in the past`() {
      assertExistingAppointmentsInsufficient(
        input = SchedulingScenarioAsserter.SchedulingAsserterInput(
          dayOfWeek = MONDAY,
          requirementLength = Duration.ofHours(80),
          allocations = listOf(
            "ALLOC1-PROJ1-FN-MON-10:00-18:00, Starting Today-365, Ending Today-1",
          ),
          existingAppointments = emptyList(),
        ),
        expectedShortfall = Duration.ofHours(80),
        expectedActions = emptyList(),
      )
    }

    @Disabled
    fun `DATES-END-06 Allocation end date not defined and project's expected end date is earliest`() {
      // This scenario is implicitly tested by the [SchedulingMappersTest] which will ensure
      // the correct end date is used when building the allocation data model
    }

    @Disabled
    fun `DATES-END-07 Allocation end date not defined and project's actual end date is earliest`() {
      // This scenario is implicitly tested by the [SchedulingMappersTest] which will ensure
      // the correct end date is used when building the allocation data model
    }

    @Disabled
    fun `DATES-END-08 Allocation end date not defined and availability's end date is earliest`() {
      // This scenario is implicitly tested by the [SchedulingMappersTest] which will ensure
      // the correct end date is used when building the allocation data model
    }
  }
}
