package uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.onOrAfter
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.Schedule
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulePlan
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingAction
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingExistingAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingRequest
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingRequiredAppointment

object SchedulePlanner {

  fun createPlanToRealiseSchedule(
    schedulingRequest: SchedulingRequest,
    schedule: Schedule,
  ): SchedulePlan {
    val today = schedulingRequest.today
    val existingAppointments = schedulingRequest.existingAppointments.appointments

    val toCreate = schedule.requiredAppointmentsAsOfToday
      .filter { requiredAppointment -> existingAppointments.none { it.matches(requiredAppointment) } }
      .map { SchedulingAction.CreateAppointment(it) }

    val toRetainBecauseRequired = schedule.requiredAppointmentsAsOfToday
      .mapNotNull { requiredAppointment -> existingAppointments.firstOrNull { it.matches(requiredAppointment) } }
      .map { SchedulingAction.RetainAppointment(it, "Required by Schedule") }

    val toRetainBecauseForced = schedule.forcedRetentions
      .map { SchedulingAction.RetainAppointment(it, "Forced Retention") }

    val toRetainSurplus = existingAppointments
      .asSequence()
      .filter { !it.hasOutcome }
      .filter { it.date.onOrAfter(today) }
      .filter { existingAppointment -> schedule.requiredAppointmentsAsOfToday.none { it.matches(existingAppointment) } }
      .filter { existingAppointment -> schedule.forcedRetentions.none { it.id == existingAppointment.id } }
      .map { SchedulingAction.RetainAppointment(it, "Surplus (scheduling doesn't currently remove appointments)") }

    return SchedulePlan(
      actions = toCreate + toRetainBecauseRequired + toRetainBecauseForced + toRetainSurplus,
      shortfall = schedule.shortfall,
    )
  }

  private fun SchedulingExistingAppointment.matches(requiredAppointment: SchedulingRequiredAppointment) = date == requiredAppointment.date &&
    startTime == requiredAppointment.startTime &&
    endTime == requiredAppointment.endTime &&
    projectCode == requiredAppointment.project.code &&
    allocation == requiredAppointment.allocation

  private fun SchedulingRequiredAppointment.matches(existingAppointment: SchedulingExistingAppointment) = date == existingAppointment.date &&
    startTime == existingAppointment.startTime &&
    endTime == existingAppointment.endTime &&
    project.code == existingAppointment.projectCode &&
    allocation == existingAppointment.allocation
}
