package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.scheduling

import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentEventService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.LockService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingAppointmentDomainEventHandler
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingTrigger
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingTriggerType
import java.time.Duration
import java.util.UUID

@ExtendWith(MockKExtension::class)
class SchedulingAppointmentDomainEventHandlerTest {

  @RelaxedMockK
  lateinit var scheduleService: SchedulingService

  @RelaxedMockK
  lateinit var appointmentEventService: AppointmentEventService

  lateinit var service: SchedulingAppointmentDomainEventHandler

  companion object {
    val EVENT_ID: UUID = UUID.randomUUID()
    val SCHEDULE_ID: UUID = UUID.randomUUID()
  }

  @BeforeEach
  fun setupService() {
    service = SchedulingAppointmentDomainEventHandler(
      scheduleService = scheduleService,
      schedulingDryRun = false,
      lockService = NoLockLockService(),
      appointmentEventService = appointmentEventService,
    )
  }

  @ParameterizedTest
  @CsvSource(
    "UPDATE,AppointmentChange",
    "CREATE,AppointmentCreated",
  )
  fun `scheduling triggered`(
    eventType: AppointmentEventType,
    expectedScheduleTriggerType: SchedulingTriggerType,
  ) {
    every {
      appointmentEventService.getEvent(EVENT_ID)
    } returns AppointmentEventEntity.valid().copy(
      crn = "CRN1",
      deliusEventNumber = 5,
      eventType = eventType,
      triggerType = AppointmentEventTriggerType.USER,
    )

    every {
      scheduleService.scheduleAppointments(any(), any(), any(), any())
    } returns SCHEDULE_ID

    service.handleAppointmentEvent(
      eventId = EVENT_ID,
      maxProcessingTime = Duration.ofSeconds(30),
    )

    verify {
      scheduleService.scheduleAppointments(
        crn = "CRN1",
        eventNumber = 5,
        trigger = SchedulingTrigger(
          type = expectedScheduleTriggerType,
          description = "Domain Event $EVENT_ID",
        ),
        dryRun = false,
      )

      appointmentEventService.recordSchedulingRan(EVENT_ID, SCHEDULE_ID)
    }
  }

  @Test
  fun `do not trigger scheduling if the appointment event was triggered from scheduling`() {
    every {
      appointmentEventService.getEvent(EVENT_ID)
    } returns AppointmentEventEntity.valid().copy(
      crn = "CRN1",
      deliusEventNumber = 5,
      triggerType = AppointmentEventTriggerType.SCHEDULING,
    )

    service.handleAppointmentEvent(
      eventId = EVENT_ID,
      maxProcessingTime = Duration.ofSeconds(30),
    )

    verify(exactly = 0) {
      scheduleService.scheduleAppointments(any(), any(), any(), any())
      appointmentEventService.recordSchedulingRan(any(), any())
    }
  }

  class NoLockLockService : LockService {
    override fun <T> withDistributedLock(
      key: String,
      waitTime: Duration,
      leaseTime: Duration,
      exec: () -> T,
    ) = exec()
  }
}
