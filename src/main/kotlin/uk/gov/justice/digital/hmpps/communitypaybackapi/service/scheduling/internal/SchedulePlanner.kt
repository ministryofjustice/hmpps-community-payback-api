package uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal

import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.Schedule
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulePlan
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingAction
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingExistingAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingRequest
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingRequiredAppointment
import java.time.LocalDate

object SchedulePlanner {

  fun createPlanToRealiseSchedule(
    schedulingRequest: SchedulingRequest,
    schedule: Schedule,
  ): SchedulePlan {
    val today = schedulingRequest.today
    val existingAppointments = schedulingRequest.existingAppointments.appointments

    val toCreate = schedule.appointments
      .filter { it.date == today || it.date.isAfter(today) }
      .filter { requiredAppointment -> !existingAppointments.any { it.equalsRequired(requiredAppointment) } }
      .map { SchedulingAction.CreateAppointment(it) }

    val toRetainRequired = schedule.appointments
      .filter { it.date.onOrAfter(today) }
      .mapNotNull { requiredAppointment -> existingAppointments.firstOrNull { it.equalsRequired(requiredAppointment) } }
      .map { SchedulingAction.RetainAppointment(it, "Required by Schedule") }

    val toRetainSurplus = existingAppointments
      .filter { !it.hasOutcome }
      .filter { it.date.onOrAfter(today) }
      .filter { existingAppointment -> schedule.appointments.none { it.equalsExisting(existingAppointment) } }
      .map { SchedulingAction.RetainAppointment(it, "Surplus (scheduling doesn't currently remove appointments)") }

    return SchedulePlan(
      actions = toCreate + toRetainRequired + toRetainSurplus,
      shortfall = schedule.shortfall,
    )
  }

  private fun LocalDate.onOrAfter(other: LocalDate) = this == other || this.isAfter(other)

  private fun SchedulingExistingAppointment.equalsRequired(requiredAppointment: SchedulingRequiredAppointment) = date == requiredAppointment.date &&
    startTime == requiredAppointment.startTime &&
    endTime == requiredAppointment.endTime &&
    project == requiredAppointment.project &&
    allocation == requiredAppointment.allocation

  private fun SchedulingRequiredAppointment.equalsExisting(existingAppointment: SchedulingExistingAppointment) = date == existingAppointment.date &&
    startTime == existingAppointment.startTime &&
    endTime == existingAppointment.endTime &&
    project == existingAppointment.project &&
    allocation == existingAppointment.allocation
}
