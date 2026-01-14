package uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal.SchedulePlanExecutor
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal.Scheduler
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal.Scheduler.SchedulerOutcome.ExistingAppointmentsInsufficient
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal.Scheduler.SchedulerOutcome.ExistingAppointmentsSufficient
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal.Scheduler.SchedulerOutcome.RequirementAlreadySatisfied
import java.time.Clock
import java.time.LocalDate

/**
 * In NDelius scheduling is triggered when a user makes a change to a person's
 * Allocation or Appointment. Scheduling will ensure, where possible,
 * that there are sufficient Appointments to satisfy the event's unpaid work
 * requirement's remaining time.
 *
 * Scheduling is driven by Allocations, which identify when a person is available
 * to work on a specific project. Allocations apply to a day of the week and are
 * repeated once, weekly or fortnightly. They also have a start and optional
 * end date.
 *
 * Scheduling is only considered with appointment as of the current day. Any existing
 * appointments as of today with outcomes already recorded are considered immutable
 * and will always be included in the schedule
 *
 * The NDelius scheduling code cannot be triggered by the `community-payback-and-delius` API,
 * so instead we duplicate it here.
 *
 * Currently, scheduling is only supported for appointment changes meaning that
 * appointments will only ever be created to satisfy a shortfall in future appointments.
 */
@Service
class SchedulingService(
  val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
  val scheduler: Scheduler,
  val schedulePlanExecutor: SchedulePlanExecutor,
  val clock: Clock,
) {

  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun scheduleAppointments(
    crn: String,
    eventNumber: Int,
    trigger: String,
    dryRun: Boolean,
  ) {
    val requirement = communityPaybackAndDeliusClient.getUnpaidWorkRequirement(crn, eventNumber)

    val schedulingRequest = SchedulingRequest(
      today = LocalDate.now(clock),
      trigger = trigger,
      requirement = requirement.requirementProgress.toSchedulingRequirement(crn, eventNumber),
      allocations = requirement.allocations.toSchedulingAllocations(),
      existingAppointments = requirement.appointments.toSchedulingExistingAppointments(),
      nonWorkingDates = SchedulingNonWorkingDates(communityPaybackAndDeliusClient.getNonWorkingDays()),
      dryRun = dryRun,
    )

    val schedulingOutcome = scheduler.producePlan(schedulingRequest)

    if (dryRun) {
      log.warn("Not applying schedule for $crn because dry run was requested")
      return
    }

    when (schedulingOutcome) {
      is ExistingAppointmentsInsufficient -> schedulePlanExecutor.executePlan(schedulingOutcome.plan)
      is ExistingAppointmentsSufficient, RequirementAlreadySatisfied -> Unit // deliberately do nothing
    }
  }
}
