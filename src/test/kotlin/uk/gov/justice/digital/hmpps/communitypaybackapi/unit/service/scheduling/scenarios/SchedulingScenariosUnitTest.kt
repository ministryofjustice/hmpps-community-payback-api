package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.scheduling.scenarios

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.scheduling.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingProject
import java.time.Duration

abstract class SchedulingScenariosUnitTest {

  private lateinit var schedulingAsserter: SchedulingScenarioAsserter

  @BeforeEach
  fun setupAsserter(testInfo: TestInfo?) {
    schedulingAsserter = SchedulingScenarioAsserter(
      listOf(
        SchedulingProject.valid().copy(code = "PROJ1"),
        SchedulingProject.valid().copy(code = "PROJ2"),
        SchedulingProject.valid().copy(code = "PROJ3"),
        SchedulingProject.valid().copy(code = "PROJ4"),
        SchedulingProject.valid().copy(code = "PROJ5"),
        SchedulingProject.valid().copy(code = "PROJ6"),
      ),
      testInfo = testInfo,
    )
  }

  fun assertRequirementAlreadySatisfied(
    input: SchedulingScenarioAsserter.SchedulingAsserterInput,
  ) = schedulingAsserter.assertRequirementAlreadySatisfied(input)

  fun assertExistingAppointmentsInsufficient(
    input: SchedulingScenarioAsserter.SchedulingAsserterInput,
    expectedShortfall: Duration = Duration.ZERO,
    expectedActions: List<String>,
  ) = schedulingAsserter.assertExistingAppointmentsInsufficient(input, expectedShortfall, expectedActions)

  fun assertExistingAppointmentsSufficient(
    input: SchedulingScenarioAsserter.SchedulingAsserterInput,
  ) = schedulingAsserter.assertExistingAppointmentsSufficient(input)
}
