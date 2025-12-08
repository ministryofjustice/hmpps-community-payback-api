package uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingService.SchedulingOutcome.ExistingAppointmentsInsufficient
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingService.SchedulingOutcome.RequirementAlreadySatisfied
import java.time.Duration

@Service
class SchedulingService {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  /**
   * Scheduling attempts to ensure there are sufficient future Appointments for a given requirement to satisfy the
   * requirement's remaining time.
   *
   * This function works as follows:
   *
   * 1. Determine if scheduling is required. If it isn't, immediately return an outcome of type [RequirementAlreadySatisfied]
   * 2. Schedule - Create a schedule of future appointments (including today) to satisfy the remaining time (or as much as possible)
   * 3. Plan - Determine the actions required in NDelius to realise the Schedule (e.g. add appointments)
   * 4. Return [ExistingAppointmentsInsufficient] which includes the Plan
   *
   * It is then the responsibility of the calling code to apply the actions
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
      return RequirementAlreadySatisfied
    }

    val schedule = ScheduleCreationService.createSchedule(
      schedulingRequest = request,
      remainingMinutesToBeScheduled = request.calculateRemainingMinutesAsOfToday(),
    )

    val plan = SchedulePlanningService.createPlanToRealiseSchedule(
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

    if (plan.actions.isEmpty() && schedule.shortfall == Duration.ZERO) {
      return SchedulingOutcome.ExistingAppointmentsSufficient
    }

    return ExistingAppointmentsInsufficient(
      plan = plan,
    )
  }

  private fun SchedulingRequest.calculateRemainingMinutesAsOfToday(): Duration {
    val lengthMinutes = requirement.length.toMinutes()

    val pastMinutesOffered = pastAppointments()
      .filter { !it.hasOutcome }
      .sumOf { minutesBetween(it.startTime, it.endTime).toMinutes() }

    val allMinutesCredited = existingAppointments.appointments
      .filter { it.hasOutcome }
      .sumOf { it.timeCredited?.toMinutes() ?: 0 }

    return Duration.ofMinutes(lengthMinutes - pastMinutesOffered - allMinutesCredited)
  }

  private fun SchedulingRequest.pastAppointments() = existingAppointments.appointments.filter { it.date.isBefore(today) }

  sealed interface SchedulingOutcome {
    object RequirementAlreadySatisfied : SchedulingOutcome
    object ExistingAppointmentsSufficient : SchedulingOutcome
    data class ExistingAppointmentsInsufficient(
      val plan: SchedulePlan,
    ) : SchedulingOutcome
  }
}
