package uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentEventService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.LockService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal.SchedulingTrigger
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal.SchedulingTriggerType
import java.time.Duration
import java.util.UUID

@Service
class SchedulingDomainEventHandler(
  private val scheduleService: SchedulingService,
  @param:Value("\${community-payback.appointment-scheduling.dryRun:false}")
  private val schedulingDryRun: Boolean,
  private val lockService: LockService,
  private val appointmentEventService: AppointmentEventService,
) {
  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  /**
   * Instead of having a database transaction around this entire call, we instead have transactions
   * around the 'batch create appointment' calls.
   *
   * This is because if we create an appointment and scheduling then errors, these appointments still
   * exist and will be considered and not recreated when scheduling is retried
   */
  fun handleAppointmentEvent(
    eventId: UUID,
    maxProcessingTime: Duration,
  ) {
    val appointmentEvent = appointmentEventService.getEvent(eventId) ?: error("Can't find event with id '$eventId'")

    if (appointmentEvent.triggerType == AppointmentEventTriggerType.SCHEDULING) {
      log.debug("not triggering scheduling for event {} because it was itself triggered via scheduling", eventId)
      return
    }

    val schedulingId = triggerScheduling(
      appointment = appointmentEvent.appointment,
      eventId = eventId,
      maxProcessingTime = maxProcessingTime,
      triggerType = when (appointmentEvent.eventType) {
        AppointmentEventType.UPDATE -> SchedulingTriggerType.AppointmentChange
        AppointmentEventType.CREATE -> SchedulingTriggerType.AppointmentCreated
      },
    )

    appointmentEventService.recordSchedulingRan(eventId, schedulingId)
  }

  private fun triggerScheduling(
    appointment: AppointmentEntity,
    eventId: UUID,
    maxProcessingTime: Duration,
    triggerType: SchedulingTriggerType,
  ): UUID = lockService.withDistributedLock(
    key = appointment.crn,
    leaseTime = maxProcessingTime,
  ) {
    scheduleService.scheduleAppointments(
      crn = appointment.crn,
      eventNumber = appointment.deliusEventNumber,
      trigger = SchedulingTrigger(
        type = triggerType,
        description = "Domain Event $eventId",
      ),
      dryRun = schedulingDryRun,
    )
  }
}
