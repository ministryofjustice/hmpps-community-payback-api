package uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulePlan
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingAction
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
    plan.actions.forEach {
      when (it) {
        is SchedulingAction.CreateAppointment -> createAppointment(it)
        is SchedulingAction.RetainAppointment -> Unit // deliberately do nothing
      }
    }
  }

  private fun createAppointment(action: SchedulingAction.CreateAppointment) {
    val toCreate = action.toCreate

    log.info("Creating appointment for allocation ${toCreate.allocation?.alias} on ${toCreate.date}")

    deliusClient.createAppointment(
      projectCode = toCreate.project.code,
      createAppointment = toCreate.toNDCreateAppointment(),
    )
  }
}
