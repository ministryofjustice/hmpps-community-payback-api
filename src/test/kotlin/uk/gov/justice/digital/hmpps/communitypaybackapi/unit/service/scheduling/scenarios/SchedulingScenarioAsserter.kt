package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.scheduling.scenarios

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.TestInfo
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.scheduling.empty
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingAction
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingAllocations
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingExistingAppointments
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingNonWorkingDates
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingRequest
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingRequirement
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal.Scheduler
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal.Scheduler.SchedulerOutcome
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

class SchedulingScenarioAsserter(
  val projects: List<SchedulingProject>,
  val testInfo: TestInfo? = null,
) {
  val testDataParsers = SchedulingScenarioParsers()
  val scheduler = Scheduler()

  fun assertRequirementAlreadySatisfied(
    input: SchedulingAsserterInput,
  ) {
    val result = scheduler.producePlan(input.toSchedulingRequest())

    assertThat(result).isInstanceOf(SchedulerOutcome.RequirementAlreadySatisfied::class.java)
  }

  fun assertExistingAppointmentsInsufficient(
    input: SchedulingAsserterInput,
    expectedShortfall: Duration = Duration.ZERO,
    expectedActions: List<String>,
  ) {
    val schedulingRequest = input.toSchedulingRequest()
    val expectedActions = expectedActions.map {
      testDataParsers.parseActionDescription(
        today = schedulingRequest.today,
        projects = projects,
        allocations = schedulingRequest.allocations.allocations,
        description = it,
      )
    }

    val result = scheduler.producePlan(schedulingRequest)

    assertThat(result).isInstanceOf(SchedulerOutcome.ExistingAppointmentsInsufficient::class.java)
    result as SchedulerOutcome.ExistingAppointmentsInsufficient

    assertThat(result.plan.shortfall).isEqualTo(expectedShortfall)
    assertThat(result.plan.actions.filterIsInstance<SchedulingAction.CreateAppointment>()).containsExactlyInAnyOrderElementsOf(expectedActions)
  }

  fun assertExistingAppointmentsSufficient(
    input: SchedulingAsserterInput,
  ) {
    val result = scheduler.producePlan(input.toSchedulingRequest())

    assertThat(result).isInstanceOf(SchedulerOutcome.ExistingAppointmentsSufficient::class.java)
  }

  private fun SchedulingAsserterInput.toSchedulingRequest(): SchedulingRequest {
    val day = findNextDateForDayOfWeek(dayOfWeek)

    val allocations = allocations.map { testDataParsers.parseAllocationDescription(day, projects, it) }

    val existingAppointments = existingAppointments.map {
      testDataParsers.parseExistingAppointmentDescription(
        today = day,
        allocations = allocations,
        description = it,
      )
    }

    val nonWorkingDates = nonWorkingDates.map { testDataParsers.parseNonWorkingDates(day, it) }

    return SchedulingRequest.empty().copy(
      today = day,
      trigger = "Unit Test '${testInfo?.displayName}'",
      requirement = SchedulingRequirement(
        crn = "CRN1",
        deliusEventNumber = 5,
        requirementLengthMinutes = requirementLength,
      ),
      allocations = SchedulingAllocations(allocations),
      existingAppointments = SchedulingExistingAppointments(existingAppointments),
      nonWorkingDates = SchedulingNonWorkingDates(nonWorkingDates),
    )
  }

  data class SchedulingAsserterInput(
    val dayOfWeek: DayOfWeek,
    val requirementLength: Duration,
    val allocations: List<String>,
    val existingAppointments: List<String>,
    val nonWorkingDates: List<String> = emptyList(),
  )

  private fun findNextDateForDayOfWeek(dayOfWeek: DayOfWeek): LocalDate = LocalDate.now().with(TemporalAdjusters.nextOrSame(dayOfWeek))
}
