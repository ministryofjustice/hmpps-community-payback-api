package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.scheduling

import org.assertj.core.api.Assertions.assertThat
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.scheduling.empty
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingAllocations
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingExistingAppointments
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingNonWorkingDates
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingRequest
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingRequirement
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingService.SchedulingOutcome
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

class CoreSchedulingAsserter(
  val projects: List<SchedulingProject>,
) {
  val service = SchedulingService()
  val testDataParsers = SchedulingTestDataParsers()

  fun assertRequirementAlreadySatisfied(
    input: SchedulingAsserterInput,
  ) {
    val result = service.producePlan(input.toSchedulingRequest())

    assertThat(result).isInstanceOf(SchedulingOutcome.RequirementAlreadySatisfied::class.java)
  }

  fun assertExistingAppointmentsInsufficient(
    input: SchedulingAsserterInput,
    expectedActions: List<String>,
  ) {
    val schedulingRequest = input.toSchedulingRequest()
    val expectedActions = expectedActions.map {
      testDataParsers.parseActionDescription(
        today = schedulingRequest.today,
        allocations = schedulingRequest.allocations.allocations,
        description = it,
      )
    }

    val result = service.producePlan(schedulingRequest)

    assertThat(result).isInstanceOf(SchedulingOutcome.ExistingAppointmentsInsufficient::class.java)
    result as SchedulingOutcome.ExistingAppointmentsInsufficient

    assertThat(result.plan.actions).isEqualTo(expectedActions)
  }

  fun assertExistingAppointmentsSufficient(
    input: SchedulingAsserterInput,
  ) {
    val result = service.producePlan(input.toSchedulingRequest())

    assertThat(result).isInstanceOf(SchedulingOutcome.ExistingAppointmentsSufficient::class.java)
  }

  private fun SchedulingAsserterInput.toSchedulingRequest(): SchedulingRequest {
    val day = findNextDateForDayOfWeek(dayOfWeek)

    val allocations = allocations.map { testDataParsers.parseAllocationDescription(day, projects, it) }
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
      requirement = SchedulingRequirement(requirementLength),
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
