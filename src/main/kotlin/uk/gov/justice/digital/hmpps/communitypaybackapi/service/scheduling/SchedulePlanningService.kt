package uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling

object SchedulePlanningService {

  fun createPlanToRealiseSchedule(
    schedulingRequest: SchedulingRequest,
    schedule: Schedule,
  ): SchedulePlan {
    val today = schedulingRequest.today
    val existingAppointments = schedulingRequest.existingAppointments.appointments

    val actions = schedule.appointments
      .filter { it.date == today || it.date.isAfter(today) }
      .filter { proposedAppointment -> !existingAppointments.any { it.date == proposedAppointment.date } }
      .map { SchedulingAction.SchedulingActionNewAppointment(it) }

    return SchedulePlan(
      actions = actions,
    )
  }
}
