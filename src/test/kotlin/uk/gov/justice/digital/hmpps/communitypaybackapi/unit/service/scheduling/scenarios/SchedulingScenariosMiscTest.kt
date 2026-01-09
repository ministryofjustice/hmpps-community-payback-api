package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.scheduling.scenarios

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.scheduling.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingProject
import java.time.DayOfWeek.MONDAY
import java.time.Duration

class SchedulingScenariosMiscTest : SchedulingScenariosUnitTest() {

  val schedulingAsserter = SchedulingScenarioAsserter(
    listOf(
      SchedulingProject.valid().copy(code = "PROJ1"),
      SchedulingProject.valid().copy(code = "PROJ2"),
    ),
  )

  @Test
  fun `MISC-01 Insufficient Allocations to meet requirements`() {
    assertExistingAppointmentsInsufficient(
      input = SchedulingScenarioAsserter.SchedulingAsserterInput(
        dayOfWeek = MONDAY,
        requirementLength = Duration.ofHours(160),
        allocations = listOf(
          "ALLOC1-PROJ1-WK-MON-10:00-18:00, Ending Today+29",
          "ALLOC2-PROJ2-WK-WED-11:00-15:00, Ending Today+17",
        ),
        existingAppointments = emptyList(),
      ),
      expectedShortfall = Duration.ofHours(108),
      expectedActions = listOf(
        "Create, Today, PROJ1, ALLOC1, 10:00-18:00",
        "Create, Today+2, PROJ2, ALLOC2, 11:00-15:00",
        "Create, Today+7, PROJ1, ALLOC1, 10:00-18:00",
        "Create, Today+9, PROJ2, ALLOC2, 11:00-15:00",
        "Create, Today+14, PROJ1, ALLOC1, 10:00-18:00",
        "Create, Today+16, PROJ2, ALLOC2, 11:00-15:00",
        "Create, Today+21, PROJ1, ALLOC1, 10:00-18:00",
        "Create, Today+28, PROJ1, ALLOC1, 10:00-18:00",
      ),
    )
  }

  @Disabled
  fun `MISC-02 Ignore Allocations if start time == end time`() {
    // This scenario is implicitly tested by the [SchedulingMappersTest] which
    // ensures allocations where startTime == endTime are discarded when
    // mapping them to the internal model
  }

  @Test
  fun `MISC-03 Maximum Requirement Length`() {
    assertExistingAppointmentsInsufficient(
      input = SchedulingScenarioAsserter.SchedulingAsserterInput(
        dayOfWeek = MONDAY,
        requirementLength = Duration.ofHours(300),
        allocations = listOf(
          "ALLOC1-PROJ1-WK-MON-10:00-14:00",
        ),
        existingAppointments = emptyList(),
      ),
      expectedActions = 0.rangeTo(74).map {
        "Create, Today+${it * 7}, PROJ1, ALLOC1, 10:00-14:00"
      },
    )
  }

  @Test
  fun `MISC-04 If multiple allocations on same day, schedule earliest start time first`() {
    assertExistingAppointmentsInsufficient(
      input = SchedulingScenarioAsserter.SchedulingAsserterInput(
        dayOfWeek = MONDAY,
        requirementLength = Duration.ofHours(8),
        allocations = listOf(
          "ALLOC1-PROJ1-WK-WED-10:00-18:00",
          "ALLOC2-PROJ2-WK-WED-08:00-16:00",
        ),
        existingAppointments = emptyList(),
      ),
      expectedActions = listOf(
        "Create, Today+2, PROJ2, ALLOC2, 08:00-16:00",
      ),
    )
  }
}
