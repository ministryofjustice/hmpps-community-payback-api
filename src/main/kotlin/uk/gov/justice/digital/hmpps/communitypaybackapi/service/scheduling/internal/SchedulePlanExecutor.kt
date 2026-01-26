package uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAppointmentsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentCreationService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulePlan
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingAction
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingRequiredAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.toCreateAppointmentDto

@Service
class SchedulePlanExecutor(
  val appointmentCreationService: AppointmentCreationService,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun executePlan(
    plan: SchedulePlan,
  ) {
    plan.actions
      .filterIsInstance<SchedulingAction.CreateAppointment>()
      .groupBy { it.toCreate.project.code }
      .forEach { (projectCode, appointmentsToCreate) ->
        createAppointment(
          plan = plan,
          projectCode = projectCode,
          toCreate = appointmentsToCreate.map { it.toCreate },
        )
      }
  }

  private fun createAppointment(
    plan: SchedulePlan,
    projectCode: String,
    toCreate: List<SchedulingRequiredAppointment>,
  ) {
    log.info("Creating ${toCreate.size} appointments for project $projectCode")

    appointmentCreationService.createAppointments(
      CreateAppointmentsDto(
        projectCode = projectCode,
        appointments = toCreate.map {
          it.toCreateAppointmentDto(
            crn = plan.crn,
            eventNumber = plan.eventNumber,
          )
        },
      ),
      triggeredBySchedulingId = plan.schedulingId,
    )
  }
}
