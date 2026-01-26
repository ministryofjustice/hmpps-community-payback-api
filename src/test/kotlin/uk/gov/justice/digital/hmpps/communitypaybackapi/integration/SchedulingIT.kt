package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCreatedAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDRequirementProgress
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingAllocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingDayOfWeek
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingExistingAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingFrequency
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDUnpaidWorkRequirement
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.findNextOrSameDateForDayOfWeek
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.validNoEndDate
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.validWithOutcome
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.MockSentryService
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.CommunityPaybackAndDeliusMockServer
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.SqsListenerException
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdditionalInformationType
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.DomainEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.DomainEventPublisher
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.HmmpsEventPersonReferences
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.HmppsAdditionalInformation
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.HmppsDomainEvent
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.Int

class SchedulingIT : IntegrationTestBase() {

  @Autowired
  lateinit var applicationContext: ApplicationContext

  @Autowired
  lateinit var domainEventPublisher: DomainEventPublisher

  @Autowired
  lateinit var appointmentEventEntityRepository: AppointmentEventEntityRepository

  @Autowired
  lateinit var mockSentryService: MockSentryService

  companion object {
    const val CRN: String = "CRN01"
    const val EVENT_NUMBER: Int = 10
  }

  @Nested
  inner class SchedulingOnAppointmentUpdate {

    @Test
    fun `Can't find update record, raise alert`() {
      val updateId = UUID.randomUUID()

      publishAppointmentUpdateDomainEvent(eventId = updateId)

      assertThat(mockSentryService.getRaisedException())
        .isInstanceOf(SqsListenerException::class.java)
        .hasMessageMatching("Error occurred handling message with ID '.*' - Can't find appointment updated record for event id '$updateId'")
    }

    @Test
    fun `Schedule already sufficient, do nothing`() {
      val schedulingDate = setClockToDayOfWeek(DayOfWeek.MONDAY)

      CommunityPaybackAndDeliusMockServer.getNonWorkingDays(emptyList())

      // ALLOC1-PROJ1-WK-MON-10:00-20:00
      val allocation1 = NDSchedulingAllocation.valid().copy(
        id = 1L,
        projectAvailability = null,
        project = NDSchedulingProject.validNoEndDate().copy(code = "PROJ1"),
        startDateInclusive = schedulingDate.minusDays(200),
        endDateInclusive = null,
        frequency = NDSchedulingFrequency.Weekly,
        dayOfWeek = NDSchedulingDayOfWeek.Monday,
        startTime = LocalTime.of(10, 0),
        endTime = LocalTime.of(18, 0),
      )

      CommunityPaybackAndDeliusMockServer.getUnpaidWorkRequirement(
        crn = CRN,
        eventNumber = EVENT_NUMBER,
        NDUnpaidWorkRequirement(
          requirementProgress = NDRequirementProgress(
            requiredMinutes = Duration.ofHours(22).toMinutes().toInt(),
            completedMinutes = 0,
            adjustments = -Duration.ofHours(2).toMinutes().toInt(),
          ),
          allocations = listOf(allocation1),
          appointments = listOf(
            // Today-14, ALLOC1, 10:00-18:00, 08:00 Credited
            NDSchedulingExistingAppointment.validWithOutcome().copy(
              date = schedulingDate.minusDays(14),
              allocationId = allocation1.id,
              startTime = LocalTime.of(10, 0),
              endTime = LocalTime.of(18, 0),
              minutesCredited = Duration.ofHours(8).toMinutes(),
            ),
            // Today-7, ALLOC1, 10:00-18:00, 08:00 Credited
            NDSchedulingExistingAppointment.validWithOutcome().copy(
              date = schedulingDate.minusDays(7),
              allocationId = allocation1.id,
              startTime = LocalTime.of(10, 0),
              endTime = LocalTime.of(18, 0),
              minutesCredited = Duration.ofHours(8).toMinutes(),
            ),
            // Today, ALLOC1, 10:00-18:00, 04:00 Credited
            NDSchedulingExistingAppointment.validWithOutcome().copy(
              date = schedulingDate,
              allocationId = allocation1.id,
              startTime = LocalTime.of(13, 0),
              endTime = LocalTime.of(20, 0),
              minutesCredited = Duration.ofHours(4).toMinutes(),
            ),
          ),
        ),
      )

      val outcomeRecordId = appointmentEventEntityRepository.save(
        AppointmentEventEntity.valid(applicationContext).copy(
          crn = CRN,
          deliusEventNumber = EVENT_NUMBER,
        ),
      ).id

      publishAppointmentUpdateDomainEvent(outcomeRecordId)
      waitForSchedulingToRun(outcomeRecordId)

      CommunityPaybackAndDeliusMockServer.postAppointmentsVerifyZeroCalls()
    }

    @Test
    fun `New appointments required after shortfall created by non attendance`() {
      val schedulingDate = setClockToDayOfWeek(DayOfWeek.WEDNESDAY)

      // This will block ALLOC1 on this specific date
      CommunityPaybackAndDeliusMockServer.getNonWorkingDays(listOf(schedulingDate.plusDays(17)))

      // ALLOC1-PROJ1-FN-SAT-10:00-16:00, Started Today-365
      val allocation1 = NDSchedulingAllocation.valid().copy(
        id = 1L,
        projectAvailability = null,
        project = NDSchedulingProject.validNoEndDate().copy(code = "PROJ1"),
        startDateInclusive = schedulingDate.minusDays(365),
        endDateInclusive = null,
        frequency = NDSchedulingFrequency.Fortnightly,
        dayOfWeek = NDSchedulingDayOfWeek.Saturday,
        startTime = LocalTime.of(10, 0),
        endTime = LocalTime.of(14, 0),
      )

      // ALLOC2-PROJ1-FN-SUN-12:00-18:00, Started Today-365
      val allocation2 = NDSchedulingAllocation.valid().copy(
        id = 2L,
        projectAvailability = null,
        project = NDSchedulingProject.validNoEndDate().copy(code = "PROJ2"),
        startDateInclusive = schedulingDate.minusDays(365),
        endDateInclusive = null,
        frequency = NDSchedulingFrequency.Fortnightly,
        dayOfWeek = NDSchedulingDayOfWeek.Sunday,
        startTime = LocalTime.of(12, 0),
        endTime = LocalTime.of(18, 0),
      )

      CommunityPaybackAndDeliusMockServer.getUnpaidWorkRequirement(
        crn = CRN,
        eventNumber = EVENT_NUMBER,
        NDUnpaidWorkRequirement(
          requirementProgress = NDRequirementProgress.valid().copy(
            requiredMinutes = Duration.ofHours(52).toMinutes().toInt(),
            completedMinutes = 0,
            adjustments = 0,
          ),
          allocations = listOf(allocation1, allocation2),
          appointments = listOf(
            // Today-32, ALLOC1, 10:00-16:00, 4 Hours Credited
            NDSchedulingExistingAppointment.validWithOutcome().copy(
              date = schedulingDate.minusDays(32),
              allocationId = allocation1.id,
              startTime = LocalTime.of(10, 0),
              endTime = LocalTime.of(14, 0),
              minutesCredited = Duration.ofHours(4).toMinutes(),
            ),
            // Today-31, ALLOC2, 12:00-18:00, 4 Hours Credited
            NDSchedulingExistingAppointment.validWithOutcome().copy(
              date = schedulingDate.minusDays(31),
              allocationId = allocation2.id,
              startTime = LocalTime.of(12, 0),
              endTime = LocalTime.of(18, 0),
              minutesCredited = Duration.ofHours(4).toMinutes(),
            ),
            // Today-25, ALLOC1, 10:00-16:00, 4 Hours Credited
            NDSchedulingExistingAppointment.validWithOutcome().copy(
              date = schedulingDate.minusDays(25),
              allocationId = allocation1.id,
              startTime = LocalTime.of(10, 0),
              endTime = LocalTime.of(14, 0),
              minutesCredited = Duration.ofHours(4).toMinutes(),
            ),
            // Today-24, ALLOC2, 12:00-18:00, 4 Hours Credited
            NDSchedulingExistingAppointment.validWithOutcome().copy(
              date = schedulingDate.minusDays(24),
              allocationId = allocation2.id,
              startTime = LocalTime.of(12, 0),
              endTime = LocalTime.of(18, 0),
              minutesCredited = Duration.ofHours(4).toMinutes(),
            ),
            // Today-18, ALLOC1, 10:00-16:00, 4 Hours Credited
            NDSchedulingExistingAppointment.validWithOutcome().copy(
              date = schedulingDate.minusDays(18),
              allocationId = allocation1.id,
              startTime = LocalTime.of(10, 0),
              endTime = LocalTime.of(14, 0),
              minutesCredited = Duration.ofHours(4).toMinutes(),
            ),
            // Today-17, ALLOC2, 12:00-18:00, 4 Hours Credited
            NDSchedulingExistingAppointment.validWithOutcome().copy(
              date = schedulingDate.minusDays(17),
              allocationId = allocation2.id,
              startTime = LocalTime.of(12, 0),
              endTime = LocalTime.of(18, 0),
              minutesCredited = Duration.ofHours(4).toMinutes(),
            ),
            // Today-11, ALLOC1, 10:00-16:00, 4 Hours Credited
            NDSchedulingExistingAppointment.validWithOutcome().copy(
              date = schedulingDate.minusDays(11),
              allocationId = allocation1.id,
              startTime = LocalTime.of(10, 0),
              endTime = LocalTime.of(14, 0),
              minutesCredited = Duration.ofHours(4).toMinutes(),
            ),
            // Today-10, ALLOC2, 12:00-18:00, 4 Hours Credited
            NDSchedulingExistingAppointment.validWithOutcome().copy(
              date = schedulingDate.minusDays(10),
              allocationId = allocation2.id,
              startTime = LocalTime.of(12, 0),
              endTime = LocalTime.of(18, 0),
              minutesCredited = Duration.ofHours(4).toMinutes(),
            ),
            // Today-4, ALLOC1, 10:00-16:00, 4 Hours Credited
            NDSchedulingExistingAppointment.validWithOutcome().copy(
              date = schedulingDate.minusDays(4),
              allocationId = allocation1.id,
              startTime = LocalTime.of(10, 0),
              endTime = LocalTime.of(14, 0),
              minutesCredited = Duration.ofHours(4).toMinutes(),
            ),
            // Today-3, ALLOC2, 12:00-18:00, 4 Hours Credited
            NDSchedulingExistingAppointment.validWithOutcome().copy(
              date = schedulingDate.minusDays(3),
              allocationId = allocation2.id,
              startTime = LocalTime.of(12, 0),
              endTime = LocalTime.of(18, 0),
              minutesCredited = Duration.ofHours(4).toMinutes(),
            ),
            // Today-1, MANUAL, 12:00-22:00, Outcome pending
            // updated to be non outcome
            NDSchedulingExistingAppointment.validWithOutcome().copy(
              date = schedulingDate.minusDays(3),
              allocationId = null,
              startTime = LocalTime.of(10, 0),
              endTime = LocalTime.of(22, 0),
              minutesCredited = null,
            ),
          ),
        ),
      )

      CommunityPaybackAndDeliusMockServer.postAppointments(
        projectCode = "PROJ1",
        response = listOf(
          NDCreatedAppointment(id = 1L),
        ),
      )
      CommunityPaybackAndDeliusMockServer.postAppointments(
        projectCode = "PROJ2",
        response = listOf(
          NDCreatedAppointment(id = 2L),
          NDCreatedAppointment(id = 3L),
        ),
      )

      val outcomeRecordId = appointmentEventEntityRepository.save(
        AppointmentEventEntity.valid(applicationContext).copy(
          crn = CRN,
          deliusEventNumber = EVENT_NUMBER,
        ),
      ).id

      publishAppointmentUpdateDomainEvent(eventId = outcomeRecordId)
      waitForSchedulingToRun(outcomeRecordId)

      /*
      Today+3 - ALLOC1, 10:00-16:00
      Today+4 - ALLOC2, 12:00-18:00
      Today+18 - ALLOC2, 12:00-14:00
       */
      CommunityPaybackAndDeliusMockServer.postAppointmentVerify(
        projectCode = "PROJ1",
        expectedAppointments = listOf(
          CommunityPaybackAndDeliusMockServer.ExpectedAppointmentCreate(
            crn = CRN,
            eventNumber = EVENT_NUMBER,
            date = schedulingDate.plusDays(3),
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(14, 0),
          ),
        ),
      )
      CommunityPaybackAndDeliusMockServer.postAppointmentVerify(
        projectCode = "PROJ2",
        expectedAppointments = listOf(
          CommunityPaybackAndDeliusMockServer.ExpectedAppointmentCreate(
            crn = CRN,
            eventNumber = EVENT_NUMBER,
            date = schedulingDate.plusDays(4),
            startTime = LocalTime.of(12, 0),
            endTime = LocalTime.of(18, 0),
          ),
          CommunityPaybackAndDeliusMockServer.ExpectedAppointmentCreate(
            crn = CRN,
            eventNumber = EVENT_NUMBER,
            date = schedulingDate.plusDays(18),
            startTime = LocalTime.of(12, 0),
            endTime = LocalTime.of(14, 0),
          ),
        ),

      )
    }

    private fun setClockToDayOfWeek(dayOfWeek: DayOfWeek): LocalDate {
      val schedulingDate = LocalDate.now().findNextOrSameDateForDayOfWeek(dayOfWeek)
      clock.setNow(schedulingDate.atTime(12, 0))
      return schedulingDate
    }

    private fun waitForSchedulingToRun(outcomeRecordId: UUID) {
      await()
        .atMost(2, TimeUnit.SECONDS)
        .until { appointmentEventEntityRepository.findByIdOrNull(outcomeRecordId)!!.schedulingRanAt != null }
    }

    private fun publishAppointmentUpdateDomainEvent(
      eventId: UUID,
    ) {
      domainEventPublisher.publish(
        HmppsDomainEvent(
          eventType = DomainEventType.APPOINTMENT_UPDATED.eventType,
          version = 1,
          description = DomainEventType.APPOINTMENT_UPDATED.description,
          detailUrl = "doesnt matter",
          occurredAt = OffsetDateTime.now(),
          additionalInformation = HmppsAdditionalInformation(
            mapOf(AdditionalInformationType.EVENT_ID.name to eventId),
          ),
          personReference = HmmpsEventPersonReferences(emptyList()),
        ),
      )
    }
  }
}
