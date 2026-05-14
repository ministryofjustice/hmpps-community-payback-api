package uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal

import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.AdjustmentCreatedEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.AppointmentCreatedEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.AppointmentUpdatedEvent

@Service
class DeliusEventTelemetryPublisher(
  private val telemetryService: TelemetryService,
) {

  @EventListener
  fun onAppointmentCreated(event: AppointmentCreatedEvent) {
    telemetryService.trackEvent(
      "AppointmentEvent",
      properties = mapOf(
        "crn" to event.appointmentEntity.crn,
        "deliusAppointmentId" to event.appointmentEntity.deliusId.toString(),
        "projectType" to event.createDto.project.projectType.name,
        "region" to event.createDto.project.providerName,
        "triggeredAt" to event.trigger.triggeredAt.toString(),
        "triggeredBy" to event.trigger.triggeredBy,
        "contactOutcome" to event.createDto.contactOutcome?.name,
        "eventType" to "CREATED",
      ),
    )
  }

  @EventListener
  fun onAppointmentUpdated(event: AppointmentUpdatedEvent) {
    telemetryService.trackEvent(
      "AppointmentEvent",
      properties = mapOf(
        "crn" to event.appointmentEntity.crn,
        "deliusAppointmentId" to event.appointmentEntity.deliusId.toString(),
        "projectType" to event.updateDto.project.projectType.name,
        "region" to event.updateDto.project.providerName,
        "triggeredAt" to event.trigger.triggeredAt.toString(),
        "triggeredBy" to event.trigger.triggeredBy,
        "contactOutcome" to event.updateDto.contactOutcome?.name,
        "eventType" to "UPDATED",
      ),
    )
  }

  @EventListener
  fun onAdjustmentCreated(event: AdjustmentCreatedEvent) {
    telemetryService.trackEvent(
      "AdjustmentEvent",
      properties = mapOf(
        "crn" to event.appointmentEntity.crn,
        "deliusAppointmentId" to event.appointmentEntity.deliusId.toString(),
        "deliusAdjustmentId" to event.deliusAdjustmentId.toString(),
        "providerCode" to event.appointmentEntity.providerCode,
        "triggeredAt" to event.trigger.triggeredAt.toString(),
        "triggeredBy" to event.trigger.triggeredBy,
        "eventType" to "CREATED",
      ),
    )
  }
}
