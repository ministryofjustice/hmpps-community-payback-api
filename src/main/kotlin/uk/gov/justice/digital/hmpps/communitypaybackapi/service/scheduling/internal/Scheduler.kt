package uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal

import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulePlan
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingAction
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingRequest
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal.SchedulingRenderer
import java.time.Duration

object Scheduler {

  private val log = LoggerFactory.getLogger(this::class.java)

  /**
   * The scheduler will produce a plan to ensure there are sufficient future Appointments for a given requirement to satisfy the
   * requirement's remaining time.
   *
   * This function works as follows:
   *
   * 1. Determine if scheduling is required. If it isn't, immediately return an outcome of type [SchedulingOutcome.RequirementAlreadySatisfied]
   * 2. Schedule - Create a schedule of appointments for today and in the future required to satisfy the remaining time (or as much as possible)
   * 3. Plan - Determine the actions required in NDelius to realise the Schedule (e.g. add appointments)
   * 4. If no actions are required, return [SchedulingOutcome.ExistingAppointmentsSufficient]. Otherwise, return [SchedulingOutcome.ExistingAppointmentsInsufficient]
   *    which includes a Plan to realise the schedule
   *
   * It is then the responsibility of the calling code to execute the plan
   */
  fun producePlan(
    request: SchedulingRequest,
  ): SchedulingOutcome {
    val remainingMinutesAsOfToday = request.calculateRemainingMinutesAsOfToday()

    if (log.isDebugEnabled) {
      log.debug(SchedulingRenderer.renderRequestSummary("Applying Scheduling", request, remainingMinutesAsOfToday))
    }

    if (remainingMinutesAsOfToday.toMinutes() <= 0) {
      log.info(
        SchedulingRenderer.renderNoChangesRequired(
          request,
          remainingMinutesAsOfToday,
        ),
      )
      return SchedulingOutcome.RequirementAlreadySatisfied
    }

    val schedule = ScheduleCreator.createSchedule(
      schedulingRequest = request,
      remainingMinutesToBeScheduled = remainingMinutesAsOfToday,
    )

    val plan = SchedulePlanner.createPlanToRealiseSchedule(
      schedulingRequest = request,
      schedule = schedule,
    )

    log.info(
      SchedulingRenderer.renderScheduleAndPlan(
        request,
        remainingMinutesAsOfToday,
        schedule,
        plan,
      ),
    )

    if (!plan.changeRequired() && schedule.shortfall == Duration.ZERO) {
      return SchedulingOutcome.ExistingAppointmentsSufficient
    }

    return SchedulingOutcome.ExistingAppointmentsInsufficient(
      plan = plan,
    )
  }

  private fun SchedulingRequest.calculateRemainingMinutesAsOfToday(): Duration {
    val lengthMinutes = requirement.lengthMinutes.toMinutes()

    val pastMinutesOffered = pastAppointments()
      .filter { !it.hasOutcome }
      .sumOf { minutesBetween(it.startTime, it.endTime).toMinutes() }

    val allMinutesCredited = existingAppointments.appointments
      .filter { it.hasOutcome }
      .sumOf { it.timeCredited?.toMinutes() ?: 0 }

    return Duration.ofMinutes(lengthMinutes - pastMinutesOffered - allMinutesCredited)
  }

  private fun SchedulingRequest.pastAppointments() = existingAppointments.appointments.filter { it.date.isBefore(today) }

  private fun SchedulePlan.changeRequired() = actions.filterIsInstance<SchedulingAction.CreateAppointment>().isNotEmpty()

  sealed interface SchedulingOutcome {
    object RequirementAlreadySatisfied : SchedulingOutcome
    object ExistingAppointmentsSufficient : SchedulingOutcome
    data class ExistingAppointmentsInsufficient(
      val plan: SchedulePlan,
    ) : SchedulingOutcome
  }
}
