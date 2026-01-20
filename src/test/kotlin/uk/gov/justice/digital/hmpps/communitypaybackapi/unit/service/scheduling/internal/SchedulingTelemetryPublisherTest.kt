package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.scheduling.internal

import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.scheduling.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.TelemetryService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulePlan
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingAction
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingExistingAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingRequest
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingRequiredAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingRequirement
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingTrigger
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal.Scheduler.SchedulerOutcome
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal.SchedulingTelemetryPublisher

@ExtendWith(MockKExtension::class)
class SchedulingTelemetryPublisherTest {

  @RelaxedMockK
  lateinit var telemetryService: TelemetryService

  @InjectMockKs
  lateinit var publisher: SchedulingTelemetryPublisher

  @Test
  fun `existing appointments sufficient`() {
    publisher.publish(
      request = SchedulingRequest.valid().copy(
        requirement = SchedulingRequirement.valid().copy(crn = "CRN123"),
        trigger = SchedulingTrigger.valid().copy(type = SchedulingTriggerType.AppointmentChange),
      ),
      outcome = SchedulerOutcome.ExistingAppointmentsSufficient,
    )

    verify {
      telemetryService.trackEvent(
        name = "SchedulingComplete",
        properties = mapOf(
          "crn" to "CRN123",
          "triggerType" to "AppointmentChange",
          "outcome" to "ExistingAppointmentsSufficient",
          "appointmentCreationCount" to "0",
        ),
      )
    }
  }

  @Test
  fun `requirement already satisfied`() {
    publisher.publish(
      request = SchedulingRequest.valid().copy(
        requirement = SchedulingRequirement.valid().copy(crn = "CRN456"),
        trigger = SchedulingTrigger.valid().copy(type = SchedulingTriggerType.AppointmentChange),
      ),
      outcome = SchedulerOutcome.RequirementAlreadySatisfied,
    )

    verify {
      telemetryService.trackEvent(
        name = "SchedulingComplete",
        properties = mapOf(
          "crn" to "CRN456",
          "triggerType" to "AppointmentChange",
          "outcome" to "RequirementAlreadySatisfied",
          "appointmentCreationCount" to "0",
        ),
      )
    }
  }

  @Test
  fun `existing appointments insufficient`() {
    publisher.publish(
      request = SchedulingRequest.valid().copy(
        requirement = SchedulingRequirement.valid().copy(crn = "CRN789"),
        trigger = SchedulingTrigger.valid().copy(type = SchedulingTriggerType.AppointmentChange),
      ),
      outcome = SchedulerOutcome.ExistingAppointmentsInsufficient(
        SchedulePlan.valid().copy(
          actions = listOf(
            SchedulingAction.CreateAppointment(SchedulingRequiredAppointment.valid()),
            SchedulingAction.RetainAppointment(SchedulingExistingAppointment.valid(), notes = "doesnt matter"),
            SchedulingAction.CreateAppointment(SchedulingRequiredAppointment.valid()),
            SchedulingAction.CreateAppointment(SchedulingRequiredAppointment.valid()),
          ),
        ),
      ),
    )

    verify {
      telemetryService.trackEvent(
        name = "SchedulingComplete",
        properties = mapOf(
          "crn" to "CRN789",
          "triggerType" to "AppointmentChange",
          "outcome" to "ExistingAppointmentsInsufficient",
          "appointmentCreationCount" to "3",
        ),
      )
    }
  }
}
