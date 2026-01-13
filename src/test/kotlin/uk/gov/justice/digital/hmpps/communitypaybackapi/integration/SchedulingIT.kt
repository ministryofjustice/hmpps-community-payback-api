package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Code
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingAllocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingDayOfWeek
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingExistingAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingFrequency
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDUnpaidWorkRequirement
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.RequirementProgress
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.findNextOrSameDateForDayOfWeek
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.validNoEndDate
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.validWithOutcome
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.CommunityPaybackAndDeliusMockServer
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingService
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import kotlin.Int

/*
Currently these tests invoke scheduling directly. once the scheduling logic
is integrated into appointment change logic these tests should instead invoke
scheduling via appointment changes
 */
class SchedulingIT : IntegrationTestBase() {

  @Autowired
  lateinit var schedulingService: SchedulingService

  companion object {
    const val CRN: String = "CRN01"
    const val EVENT_NUMBER: Int = 10
  }

  @Nested
  inner class AppointmentUpdate {

    @Test
    fun `UPDATE-01 New Truncated Appointment created after start time amended and outcome recorded`() {
      // DA: Once we have integrated with appointment update amend this scenario
      // to update today's appointment's start time and outcome via the API instead
      // of 'hardcoding' the outcome in the appointment definition

      val schedulingDate = LocalDate.now().findNextOrSameDateForDayOfWeek(DayOfWeek.WEDNESDAY)
      clock.setNow(schedulingDate.atTime(12, 0))

      CommunityPaybackAndDeliusMockServer.getNonWorkingDays(listOf(schedulingDate.plusDays(7)))

      val allocation1 = NDSchedulingAllocation.valid().copy(
        id = 1L,
        projectAvailability = null,
        project = NDSchedulingProject.validNoEndDate().copy(code = Code("PROJ1")),
        startDateInclusive = LocalDate.now().minusDays(200),
        endDateInclusive = null,
        frequency = NDSchedulingFrequency.WEEKLY,
        dayOfWeek = NDSchedulingDayOfWeek.WEDNESDAY,
        startTime = LocalTime.of(10, 0),
        endTime = LocalTime.of(18, 0),
      )

      CommunityPaybackAndDeliusMockServer.getUnpaidWorkRequirement(
        crn = CRN,
        eventNumber = EVENT_NUMBER,
        NDUnpaidWorkRequirement(
          requirementProgress = RequirementProgress(
            requiredMinutes = Duration.ofHours(80).toMinutes().toInt(),
            completedMinutes = 0,
            adjustments = Duration.ofHours(2).toMinutes().toInt(),
          ),
          allocations = listOf(allocation1),
          appointments = listOf(
            NDSchedulingExistingAppointment.validWithOutcome().copy(
              date = schedulingDate.minusDays(101),
              allocation = null,
              startTime = LocalTime.of(14, 0),
              endTime = LocalTime.of(18, 0),
              minutesCredited = Duration.ofHours(4),
            ),
            NDSchedulingExistingAppointment.validWithOutcome().copy(
              date = schedulingDate.minusDays(100),
              allocation = null,
              startTime = LocalTime.of(12, 0),
              endTime = LocalTime.of(16, 0),
              minutesCredited = Duration.ofHours(4),
            ),
            NDSchedulingExistingAppointment.validWithOutcome().copy(
              date = schedulingDate.minusDays(56),
              allocation = allocation1,
              startTime = LocalTime.of(10, 0),
              endTime = LocalTime.of(18, 0),
              minutesCredited = Duration.ofHours(8),
            ),
            NDSchedulingExistingAppointment.validWithOutcome().copy(
              date = schedulingDate.minusDays(49),
              allocation = allocation1,
              startTime = LocalTime.of(10, 0),
              endTime = LocalTime.of(18, 0),
              minutesCredited = Duration.ofHours(8),
            ),
            NDSchedulingExistingAppointment.validWithOutcome().copy(
              date = schedulingDate.minusDays(42),
              allocation = allocation1,
              startTime = LocalTime.of(10, 0),
              endTime = LocalTime.of(18, 0),
              minutesCredited = Duration.ofHours(8),
            ),
            NDSchedulingExistingAppointment.validWithOutcome().copy(
              date = schedulingDate.minusDays(35),
              allocation = allocation1,
              startTime = LocalTime.of(10, 0),
              endTime = LocalTime.of(18, 0),
              minutesCredited = Duration.ofHours(8),
            ),
            NDSchedulingExistingAppointment.validWithOutcome().copy(
              date = schedulingDate.minusDays(28),
              allocation = allocation1,
              startTime = LocalTime.of(10, 0),
              endTime = LocalTime.of(18, 0),
              minutesCredited = Duration.ofHours(8),
            ),
            NDSchedulingExistingAppointment.validWithOutcome().copy(
              date = schedulingDate.minusDays(21),
              allocation = allocation1,
              startTime = LocalTime.of(10, 0),
              endTime = LocalTime.of(18, 0),
              minutesCredited = Duration.ofHours(8),
            ),
            NDSchedulingExistingAppointment.validWithOutcome().copy(
              date = schedulingDate.minusDays(14),
              allocation = allocation1,
              startTime = LocalTime.of(10, 0),
              endTime = LocalTime.of(18, 0),
              minutesCredited = Duration.ofHours(8),
            ),
            NDSchedulingExistingAppointment.validWithOutcome().copy(
              date = schedulingDate.minusDays(7),
              allocation = allocation1,
              startTime = LocalTime.of(10, 0),
              endTime = LocalTime.of(18, 0),
              minutesCredited = Duration.ofHours(8),
            ),
            NDSchedulingExistingAppointment.validWithOutcome().copy(
              date = schedulingDate,
              allocation = allocation1,
              startTime = LocalTime.of(13, 0),
              endTime = LocalTime.of(20, 0),
              minutesCredited = Duration.ofHours(7),
            ),
          ),
        ),
      )

      CommunityPaybackAndDeliusMockServer.postAppointment("PROJ1")

      schedulingService.scheduleAppointments(CRN, EVENT_NUMBER, trigger = "UPDATE-01 IT")

      CommunityPaybackAndDeliusMockServer.postAppointmentVerify(
        projectCode = "PROJ1",
        date = schedulingDate.plusDays(14),
        startTime = LocalTime.of(10, 0),
        endTime = LocalTime.of(13, 0),
      )
    }

    @Test
    fun `UPDATE-02 Requirement Complete after Appointment Outcome Recorded`() {
      // DA: To do
      // DA: Need to determine what will update the requirement status - community payback or probation-integration?
    }

    @Test
    fun `UPDATE-03 New Truncated Appointment created after Non-Attendance Outcome Recorded`() {
      // DA: To do
    }

    @Test
    fun `UPDATE-04 New Appointments created after Non-Attendance Outcome Recorded`() {
      // DA: To do
    }

    @Test
    fun `UPDATE-05 Non-triggering changes made to Requirement before Appointment Update`() {
      // DA: To do
    }
  }
}
