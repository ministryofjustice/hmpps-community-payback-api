package uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.TelemetryService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingAction
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingRequest
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal.Scheduler.SchedulerOutcome

@Service
class SchedulingTelemetryPublisher(
  val telemetryService: TelemetryService,
) {

  fun publish(
    request: SchedulingRequest,
    outcome: SchedulerOutcome,
  ) {
    telemetryService.trackEvent(
      "SchedulingComplete",
      properties = mapOf(
        "crn" to request.requirement.crn,
        "triggerType" to request.trigger.type.name,
        "outcome" to outcome::class.simpleName,
        "appointmentCreationCount" to when (outcome) {
          is SchedulerOutcome.ExistingAppointmentsInsufficient -> outcome.plan.actions.count { it is SchedulingAction.CreateAppointment }.toString()
          else -> "0"
        },
      ),
    )
  }
}
