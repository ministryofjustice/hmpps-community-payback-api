package uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal.SchedulePlanExecutor
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal.Scheduler
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal.Scheduler.SchedulingOutcome.ExistingAppointmentsInsufficient
import java.time.Clock
import java.time.LocalDate

@Service
class SchedulingService(
  val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
  val schedulePlanExecutor: SchedulePlanExecutor,
  val clock: Clock,
) {

  @SuppressWarnings("UnusedParameter")
  fun schedulePlanAndApply(
    crn: String,
    eventNumber: Int,
    trigger: SchedulingTrigger,
  ) {
    val requirement = communityPaybackAndDeliusClient.getUnpaidWorkRequirement(crn, eventNumber)

    val schedulingRequest = SchedulingRequest(
      today = LocalDate.now(clock),
      trigger = trigger,
      requirement = requirement.requirementProgress.toSchedulingRequirementProgress(),
      allocations = requirement.allocations.toSchedulingAllocations(),
      existingAppointments = requirement.appointments.toSchedulingExistingAppointments(),
      nonWorkingDates = SchedulingNonWorkingDates(communityPaybackAndDeliusClient.getNonWorkingDates()),
    )

    val schedulingOutcome = Scheduler.producePlan(schedulingRequest)

    if (schedulingOutcome is ExistingAppointmentsInsufficient) {
      schedulePlanExecutor.executePlan(schedulingOutcome.plan)
    }
  }
}
