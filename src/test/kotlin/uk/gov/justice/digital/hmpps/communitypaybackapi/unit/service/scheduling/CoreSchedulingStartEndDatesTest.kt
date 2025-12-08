package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.scheduling

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.scheduling.CoreSchedulingAsserter.SchedulingAsserterInput
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.TUESDAY
import java.time.Duration

class CoreSchedulingStartEndDatesTest {

  val coreSchedulingAsserter = CoreSchedulingAsserter(
    listOf(
      SchedulingProject(code = "PROJ1"),
      SchedulingProject(code = "PROJ2"),
    ),
  )

  @Nested
  inner class StartDate {

    @Test
    fun `DATES-START-01 Allocation Start Date is tomorrow`() {
      coreSchedulingAsserter.assertExistingAppointmentsInsufficient(
        input = SchedulingAsserterInput(
          dayOfWeek = MONDAY,
          requirementLength = Duration.ofHours(16),
          allocations = listOf(
            "ALLOC1-PROJ1-WK-TUE-10:00-18:00. Starting Today+1",
          ),
          existingAppointments = emptyList(),
        ),
        expectedActions = listOf(
          "Create, Today+1, ALLOC1, 10:00-18:00",
          "Create, Today+8, ALLOC1, 10:00-18:00",
        ),
      )
    }

    @Test
    fun `DATES-START-02 Allocation Start Date is today`() {
      coreSchedulingAsserter.assertExistingAppointmentsInsufficient(
        input = SchedulingAsserterInput(
          dayOfWeek = TUESDAY,
          requirementLength = Duration.ofHours(16),
          allocations = listOf(
            "ALLOC1-PROJ1-WK-TUE-10:00-18:00. Starting Today",
          ),
          existingAppointments = emptyList(),
        ),
        expectedActions = listOf(
          "Create, Today, ALLOC1, 10:00-18:00",
          "Create, Today+7, ALLOC1, 10:00-18:00",
        ),
      )
    }

    @Test
    fun `DATES-START-03 Allocation Start Date is in far future`() {
      coreSchedulingAsserter.assertExistingAppointmentsInsufficient(
        input = SchedulingAsserterInput(
          dayOfWeek = TUESDAY,
          requirementLength = Duration.ofHours(16),
          allocations = listOf(
            "ALLOC1-PROJ1-WK-TUE-10:00-18:00. Starting Today+700",
          ),
          existingAppointments = emptyList(),
        ),
        expectedActions = listOf(
          "Create, Today+700, ALLOC1, 10:00-18:00",
          "Create, Today+707, ALLOC1, 10:00-18:00",
        ),
      )
    }
  }

  @Nested
  inner class EndDates {

    @Test
    fun `DATES-END-01 Allocation End Date is day of next iteration`() {
      coreSchedulingAsserter.assertExistingAppointmentsInsufficient(
        input = SchedulingAsserterInput(
          dayOfWeek = MONDAY,
          requirementLength = Duration.ofHours(80),
          allocations = listOf(
            "ALLOC1-PROJ1-FN-MON-10:00-18:00, Starting Today, Ending Today+14",
          ),
          existingAppointments = emptyList(),
        ),
        expectedActions = listOf(
          "Create, Today, ALLOC1, 10:00-18:00",
          "Create, Today+14, ALLOC1, 10:00-18:00",
        ),
      )
    }

    @Test
    fun `DATES-END-02 Allocation End Date is day before next iteration`() {
      coreSchedulingAsserter.assertExistingAppointmentsInsufficient(
        input = SchedulingAsserterInput(
          dayOfWeek = MONDAY,
          requirementLength = Duration.ofHours(80),
          allocations = listOf(
            "ALLOC1-PROJ1-FN-MON-10:00-18:00, Starting Today, Ending Today+13",
          ),
          existingAppointments = emptyList(),
        ),
        expectedActions = listOf(
          "Create, Today, ALLOC1, 10:00-18:00",
        ),
      )
    }

    @SchedulingNDeliusDataModelsRequired
    fun `DATES-END-03 Allocation Start Date is same as End Date`() {
      // when data models are built this allocation should be discarded
    }

    @Test
    fun `DATES-END-04 Allocation End Date is so close to Start Date it prohibits Appointments being created`() {
      coreSchedulingAsserter.assertExistingAppointmentsInsufficient(
        input = SchedulingAsserterInput(
          dayOfWeek = TUESDAY,
          requirementLength = Duration.ofHours(80),
          allocations = listOf(
            "ALLOC1-PROJ1-FN-MON-10:00-18:00. Starting Today-1, Ending Today+12",
          ),
          existingAppointments = emptyList(),
        ),
        expectedActions = emptyList(),
      )
    }

    @Test
    fun `DATES-END-05 Allocation ends in the past`() {
      coreSchedulingAsserter.assertExistingAppointmentsInsufficient(
        input = SchedulingAsserterInput(
          dayOfWeek = MONDAY,
          requirementLength = Duration.ofHours(80),
          allocations = listOf(
            "ALLOC1-PROJ1-FN-MON-10:00-18:00, Starting Today-365, Ending Today-1",
          ),
          existingAppointments = emptyList(),
        ),
        expectedActions = emptyList(),
      )
    }

    @SchedulingNDeliusDataModelsRequired
    fun `DATES-END-06 Allocation end date not defined and project's expected end date is earliest`() {
      // End date will determined when scheduling data models are built
    }

    @SchedulingNDeliusDataModelsRequired
    fun `DATES-END-07 Allocation end date not defined and project's actual end date is earliest`() {
      // End date will determined when scheduling data models are built
    }

    @SchedulingNDeliusDataModelsRequired
    fun `DATES-END-08 Allocation end date not defined and availability's end date is earliest`() {
      // End date will determined when scheduling data models are built
    }
  }
}
