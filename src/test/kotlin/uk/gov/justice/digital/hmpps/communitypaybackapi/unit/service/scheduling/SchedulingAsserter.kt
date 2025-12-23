package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.scheduling

import org.assertj.core.api.Assertions.assertThat
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.scheduling.empty
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingAction
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingAllocations
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingExistingAppointments
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingNonWorkingDates
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingRequest
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingRequirementProgress
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal.Scheduler
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal.Scheduler.SchedulingOutcome
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

class SchedulingAsserter(
  val projects: List<SchedulingProject>,
) {
  val testDataParsers = SchedulingTestDataParsers()

  fun assertRequirementAlreadySatisfied(
    input: SchedulingAsserterInput,
  ) {
    val result = Scheduler.producePlan(input.toSchedulingRequest())

    assertThat(result).isInstanceOf(SchedulingOutcome.RequirementAlreadySatisfied::class.java)
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

    val result = Scheduler.producePlan(schedulingRequest)

    assertThat(result).isInstanceOf(SchedulingOutcome.ExistingAppointmentsInsufficient::class.java)
    result as SchedulingOutcome.ExistingAppointmentsInsufficient

    assertThat(result.plan.shortfall).isEqualTo(expectedShortfall)
    assertThat(result.plan.actions.filterIsInstance<SchedulingAction.CreateAppointment>()).containsExactlyInAnyOrderElementsOf(expectedActions)
  }

  fun assertExistingAppointmentsSufficient(
    input: SchedulingAsserterInput,
  ) {
    val result = Scheduler.producePlan(input.toSchedulingRequest())

    assertThat(result).isInstanceOf(SchedulingOutcome.ExistingAppointmentsSufficient::class.java)
  }

  private fun SchedulingAsserterInput.toSchedulingRequest(): SchedulingRequest {
    val day = findNextDateForDayOfWeek(dayOfWeek)

    // DA: build upstream allocations instead and pass through mapper
    // DA: we'd need to pass through the alias (string) somehow or we'll loose useful context :/
    val allocations = allocations.map { testDataParsers.parseAllocationDescription(day, projects, it) }

    // DA: build upstream appointments instead and pass through mapper
    val existingAppointments = existingAppointments.map {
      testDataParsers.parseExistingAppointmentDescription(
        today = day,
        allocations = allocations,
        projects = projects,
        description = it,
      )
    }

    val nonWorkingDates = nonWorkingDates.map { testDataParsers.parseNonWorkingDates(day, it) }

    return SchedulingRequest.empty().copy(
      today = day,
      requirement = SchedulingRequirementProgress(requirementLength),
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
