package uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCreateAppointments
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulePlan
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingAction
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingRequiredAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.toNDCreateAppointment

@Service
class SchedulePlanExecutor(
  val deliusClient: CommunityPaybackAndDeliusClient,
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
          crn = plan.crn,
          eventNumber = plan.eventNumber,
          projectCode = projectCode,
          toCreate = appointmentsToCreate.map { it.toCreate },
        )
      }
  }

  private fun createAppointment(
    crn: String,
    eventNumber: Int,
    projectCode: String,
    toCreate: List<SchedulingRequiredAppointment>,
  ) {
    log.info("Creating ${toCreate.size} appointments for project $projectCode")

    deliusClient.createAppointments(
      projectCode = projectCode,
      appointments = NDCreateAppointments(
        toCreate.map {
          it.toNDCreateAppointment(
            crn = crn,
            eventNumber = eventNumber,
          )
        },
      ),
    )
  }
}
