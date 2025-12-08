package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.scheduling

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.scheduling.CoreSchedulingAsserter.SchedulingAsserterInput
import java.time.DayOfWeek.MONDAY
import java.time.Duration

class CoreSchedulingInconsistentBehavioursTest {

  val coreSchedulingAsserter = CoreSchedulingAsserter(
    listOf(
      SchedulingProject(code = "PROJ1"),
      SchedulingProject(code = "PROJ2"),
    ),
  )

  @Nested
  inner class Once {

    @Test
    fun `INC-ONCE-01 'Once' allocation already scheduled last week will result in multiple appointments if end date allows`() {
      coreSchedulingAsserter.assertExistingAppointmentsInsufficient(
        input = SchedulingAsserterInput(
          dayOfWeek = MONDAY,
          requirementLength = Duration.ofHours(40),
          allocations = listOf(
            "ALLOC1-PROJ1-ONCE-MON-10:00-20:00, Starting Today-7, Ending Today+1",
          ),
          existingAppointments = listOf(
            "Today-7, PROJ1, ALLOC1, 10:00-20:00, Credited PT8H",
          ),
        ),
        expectedActions = listOf(
          "Create, Today, ALLOC1, 10:00-20:00",
        ),
      )
    }

    @Test
    fun `INC-ONCE-02 'Once' allocation already scheduled months ago will result in multiple appointments if end date allows`() {
      coreSchedulingAsserter.assertExistingAppointmentsInsufficient(
        input = SchedulingAsserterInput(
          dayOfWeek = MONDAY,
          requirementLength = Duration.ofHours(40),
          allocations = listOf(
            "ALLOC1-PROJ1-ONCE-MON-10:00-20:00, Starting Today-365, Ending Today+1",
          ),
          existingAppointments = listOf(
            "Today-365, PROJ1, ALLOC1, 10:00-20:00, Credited PT8H",
          ),
        ),
        expectedActions = listOf(
          "Create, Today, ALLOC1, 10:00-20:00",
        ),
      )
    }

    @Test
    fun `INC-ONCE-03 'Once' allocation with suitable end date will not result in multiple appointments`() {
      coreSchedulingAsserter.assertExistingAppointmentsInsufficient(
        input = SchedulingAsserterInput(
          dayOfWeek = MONDAY,
          requirementLength = Duration.ofHours(40),
          allocations = listOf(
            "ALLOC1-PROJ1-ONCE-MON-10:00-20:00, Starting Today-7, Ending Today-1",
          ),
          existingAppointments = listOf(
            "Today-7, PROJ1, ALLOC1, 10:00-20:00, Credited PT8H",
          ),
        ),
        expectedActions = emptyList(),
      )
    }
  }
}
