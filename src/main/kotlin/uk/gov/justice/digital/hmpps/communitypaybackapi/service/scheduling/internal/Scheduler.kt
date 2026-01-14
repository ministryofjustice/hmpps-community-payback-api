package uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulePlan
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingAction
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingRequest
import java.time.Duration

@Component
class Scheduler {

  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  /**
   * The scheduler will produce a plan to ensure that there are sufficient future Appointments for a given requirement to satisfy the
   * requirement's remaining time.
   *
   * This function works as follows:
   *
   * 1. If the requirement is already complete immediately return an outcome of type [SchedulerOutcome.RequirementAlreadySatisfied]
   * 2. Schedule - Create a schedule of appointments as of today required to satisfy the remaining requirement time (or as much of the time as possible)
   * 3. Plan - Determine the actions required in NDelius to realise the Schedule (e.g. add appointments)
   * 4. If no actions are required, return [SchedulerOutcome.ExistingAppointmentsSufficient]. Otherwise, return [SchedulerOutcome.ExistingAppointmentsInsufficient]
   *    which includes a Plan to realise the schedule
   *
   * It is then the responsibility of the calling code to realise the plan
   *
   * This function will also log out the current state and outcome using the [SchedulingRenderer]
   */
  fun producePlan(
    request: SchedulingRequest,
  ): SchedulerOutcome {
    val remainingMinutesAsOfToday = RequirementRemainingMinutesCalculator.calculateRemainingMinutesAsOfToday(
      request.today,
      request.requirement,
      request.existingAppointments,
    )

    if (log.isDebugEnabled) {
      log.debug(SchedulingRenderer.requestSummary("Applying Scheduling", request, remainingMinutesAsOfToday))
    }

    if (remainingMinutesAsOfToday.toMinutes() <= 0) {
      log.info(SchedulingRenderer.noChangesRequired(request, remainingMinutesAsOfToday))
      return SchedulerOutcome.RequirementAlreadySatisfied
    }

    val schedule = ScheduleCreator.createSchedule(
      schedulingRequest = request,
      remainingMinutesToBeScheduled = remainingMinutesAsOfToday,
    )

    val plan = SchedulePlanner.createPlanToRealiseSchedule(
      schedulingRequest = request,
      schedule = schedule,
    )

    log.info(SchedulingRenderer.scheduleAndPlan(request, remainingMinutesAsOfToday, schedule, plan))

    return if (!plan.changeRequired() && schedule.shortfall == Duration.ZERO) {
      SchedulerOutcome.ExistingAppointmentsSufficient
    } else {
      SchedulerOutcome.ExistingAppointmentsInsufficient(plan)
    }
  }

  private fun SchedulePlan.changeRequired() = actions.filterIsInstance<SchedulingAction.CreateAppointment>().isNotEmpty()

  sealed interface SchedulerOutcome {
    object RequirementAlreadySatisfied : SchedulerOutcome
    object ExistingAppointmentsSufficient : SchedulerOutcome
    data class ExistingAppointmentsInsufficient(
      val plan: SchedulePlan,
    ) : SchedulerOutcome
  }
}
