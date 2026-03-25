package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.scheduling

import io.mockk.Called
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDUnpaidWorkRequirement
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.scheduling.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulePlan
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingTrigger
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal.SchedulePlanExecutor
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal.Scheduler
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal.Scheduler.SchedulerOutcome
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal.SchedulingTelemetryPublisher
import java.time.Clock
import java.time.Duration

@ExtendWith(MockKExtension::class)
class SchedulingServiceTest {

  @RelaxedMockK
  lateinit var communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient

  @RelaxedMockK
  lateinit var scheduler: Scheduler

  @RelaxedMockK
  lateinit var schedulePlanExecutor: SchedulePlanExecutor

  @RelaxedMockK
  lateinit var scheduleTelemetryPublisher: SchedulingTelemetryPublisher

  @RelaxedMockK
  lateinit var clock: Clock

  @InjectMockKs
  lateinit var schedulingService: SchedulingService

  companion object {
    const val CRN: String = "CRN01"
    const val EVENT_NO: Int = 5
  }

  @Test
  fun `if existing appointments sufficient, do nothing`() {
    every { communityPaybackAndDeliusClient.getUnpaidWorkRequirement(CRN, EVENT_NO) } returns NDUnpaidWorkRequirement.valid()
    every { scheduler.producePlan(any()) } returns SchedulerOutcome.ExistingAppointmentsSufficient

    schedulingService.scheduleAppointments(CRN, EVENT_NO, SchedulingTrigger.valid(), dryRun = false)

    verify { schedulePlanExecutor wasNot Called }
    verify { scheduleTelemetryPublisher.publish(any(), SchedulerOutcome.ExistingAppointmentsSufficient) }
  }

  @Test
  fun `if requirement already satisfied, do nothing`() {
    every { communityPaybackAndDeliusClient.getUnpaidWorkRequirement(CRN, EVENT_NO) } returns NDUnpaidWorkRequirement.valid()
    every { scheduler.producePlan(any()) } returns SchedulerOutcome.RequirementAlreadySatisfied

    schedulingService.scheduleAppointments(CRN, EVENT_NO, SchedulingTrigger.valid(), dryRun = false)

    verify { schedulePlanExecutor wasNot Called }
    verify { scheduleTelemetryPublisher.publish(any(), SchedulerOutcome.RequirementAlreadySatisfied) }
  }

  @Test
  fun `if existing appointments insufficient, execute plan`() {
    every { communityPaybackAndDeliusClient.getUnpaidWorkRequirement(CRN, EVENT_NO) } returns NDUnpaidWorkRequirement.valid()

    val plan = SchedulePlan.valid().copy(
      crn = CRN,
      eventNumber = EVENT_NO,
      actions = emptyList(),
      shortfall = Duration.ofMinutes(5),
    )
    val outcome = SchedulerOutcome.ExistingAppointmentsInsufficient(plan)
    every { scheduler.producePlan(any()) } returns outcome

    schedulingService.scheduleAppointments(CRN, EVENT_NO, SchedulingTrigger.valid(), dryRun = false)

    verify { schedulePlanExecutor.executePlan(plan) }
    verify { scheduleTelemetryPublisher.publish(any(), outcome) }
  }

  @Test
  fun `if existing appointments insufficient but dry run is enabled, dont execute plan`() {
    every { communityPaybackAndDeliusClient.getUnpaidWorkRequirement(CRN, EVENT_NO) } returns NDUnpaidWorkRequirement.valid()

    val plan = SchedulePlan.valid().copy(
      crn = CRN,
      eventNumber = EVENT_NO,
      actions = emptyList(),
      shortfall = Duration.ofMinutes(5),
    )
    val outcome = SchedulerOutcome.ExistingAppointmentsInsufficient(plan)
    every { scheduler.producePlan(any()) } returns outcome

    schedulingService.scheduleAppointments(CRN, EVENT_NO, SchedulingTrigger.valid(), dryRun = true)

    verify { schedulePlanExecutor wasNot Called }
    verify { scheduleTelemetryPublisher.publish(any(), outcome) }
  }
}
