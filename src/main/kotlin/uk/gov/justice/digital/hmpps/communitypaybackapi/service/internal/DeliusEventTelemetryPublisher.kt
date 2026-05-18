package uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal

import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.AdjustmentCreatedEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.AppointmentCreatedEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.AppointmentTaskCreatedEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.AppointmentTaskUpdatedEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.AppointmentUpdatedEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.CourseCompletionProcessedEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.CourseCompletionReceivedEvent

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

  @EventListener
  fun onCourseCompletionReceived(event: CourseCompletionReceivedEvent) {
    telemetryService.trackEvent(
      "CourseCompletionEvent",
      properties = mapOf(
        "attempts" to event.attempts?.toString(),
        "courseName" to event.courseName,
        "courseType" to event.courseType,
        "provider" to event.provider,
        "region" to event.region,
        "triggeredAt" to event.triggeredAt.toString(),
        "triggeredBy" to event.triggeredBy,
        "eventType" to "RECEIVED",
      ),
    )
  }

  @EventListener
  fun onCourseCompletionProcessed(event: CourseCompletionProcessedEvent) {
    telemetryService.trackEvent(
      "CourseCompletionEvent",
      properties = mapOf(
        "crn" to event.crn,
        "resolutionType" to event.resolutionType.name,
        "triggeredAt" to event.triggeredAt.toString(),
        "triggeredBy" to event.triggeredBy,
        "eventType" to "PROCESSED",
      ),
    )
  }

  @EventListener
  fun onAppointmentTaskCreated(event: AppointmentTaskCreatedEvent) {
    telemetryService.trackEvent(
      "AppointmentTaskEvent",
      properties = mapOf(
        "crn" to event.crn,
        "deliusAppointmentId" to event.deliusAppointmentId.toString(),
        "taskType" to event.taskType.name,
        "triggeredAt" to event.triggeredAt.toString(),
        "triggeredBy" to event.triggeredBy,
        "eventType" to "CREATED",
      ),
    )
  }

  @EventListener
  fun onAppointmentTaskUpdated(event: AppointmentTaskUpdatedEvent) {
    telemetryService.trackEvent(
      "AppointmentTaskEvent",
      properties = mapOf(
        "crn" to event.crn,
        "deliusAppointmentId" to event.deliusAppointmentId.toString(),
        "taskType" to event.taskType.name,
        "taskStatus" to event.taskStatus.name,
        "triggeredAt" to event.triggeredAt.toString(),
        "triggeredBy" to event.triggeredBy,
        "eventType" to "UPDATED",
      ),
    )
  }
}
