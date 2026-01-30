package uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentEventService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.LockService
import java.time.Duration
import java.util.UUID

@Service
class SchedulingAppointmentDomainEventHandler(
  private val scheduleService: SchedulingService,
  @param:Value("\${community-payback.scheduling.dryRun:false}")
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

    val schedulingId = lockService.withDistributedLock(
      key = appointmentEvent.crn,
      leaseTime = maxProcessingTime,
    ) {
      val triggerDescription = "Domain Event $eventId"

      scheduleService.scheduleAppointments(
        crn = appointmentEvent.crn,
        eventNumber = appointmentEvent.deliusEventNumber,
        trigger = when (appointmentEvent.eventType) {
          AppointmentEventType.CREATE -> SchedulingTrigger(
            type = SchedulingTriggerType.AppointmentCreated,
            description = triggerDescription,
          )
          AppointmentEventType.UPDATE -> SchedulingTrigger(
            type = SchedulingTriggerType.AppointmentChange,
            description = triggerDescription,
          )
        },
        dryRun = schedulingDryRun,
      )
    }

    appointmentEventService.recordSchedulingRan(eventId, schedulingId)
  }
}
