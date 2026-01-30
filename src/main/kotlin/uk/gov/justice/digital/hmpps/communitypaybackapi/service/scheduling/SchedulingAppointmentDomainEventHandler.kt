package uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
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
    val domainEventDetails = appointmentEventService.getEvent(eventId)
      ?: error("Can't find appointment updated record for event id '$eventId'")

    val schedulingId = lockService.withDistributedLock(
      key = domainEventDetails.crn,
      leaseTime = maxProcessingTime,
    ) {
      scheduleService.scheduleAppointments(
        crn = domainEventDetails.crn,
        eventNumber = domainEventDetails.deliusEventNumber,
        trigger = SchedulingTrigger(
          type = SchedulingTriggerType.AppointmentChange,
          description = "Appointment Updated",
        ),
        dryRun = schedulingDryRun,
      )
    }

    appointmentEventService.recordSchedulingRan(eventId, schedulingId)
  }
}
