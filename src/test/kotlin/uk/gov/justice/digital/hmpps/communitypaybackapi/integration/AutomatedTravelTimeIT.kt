package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.reactive.server.StatusAssertions
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAdjustment
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAdjustmentRequest
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAdjustmentType
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDNameCode
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProjectAndLocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProjectType
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDUpwDetails
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AttendanceDataDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity.Companion.CODE_ATTENDED_COMPLIED
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.validNoOutcome
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.persist
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.DomainEventAsserter
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.CommunityPaybackAndDeliusMockServer
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdjustmentIdGenerator.AutomatedAdjustmentProperties
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AutomatedTravelTimeService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.DefaultAdjustmentIdGenerator
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@TestPropertySource(
  properties = [
    "adjustments.automated-travel-time.enabled=true",
    "adjustments.automated-travel-time.provider-codes=N07",
  ],
)
class AutomatedTravelTimeIT : IntegrationTestBase() {

  @Autowired
  lateinit var adjustmentEntityRepository: AdjustmentEntityRepository

  @Autowired
  lateinit var adjustmentEventEntityRepository: AdjustmentEventEntityRepository

  @Autowired
  lateinit var adjustmentReasonEntityRepository: AdjustmentReasonEntityRepository

  @Autowired
  lateinit var appointmentEntityRepository: AppointmentEntityRepository

  @Autowired
  lateinit var contactOutcomeEntityRepository: ContactOutcomeEntityRepository

  @Autowired
  lateinit var domainEventAsserter: DomainEventAsserter

  private companion object {
    const val USERNAME = "theusername"
    const val CRN = "X765432"
    const val EVENT_NUMBER = 3
    const val DELIUS_APPOINTMENT_ID = 98765L
    const val DELIUS_ADJUSTMENT_ID = 4567L
    const val LONDON = "N07"
    const val GROUP_PROJECT_TYPE = "NP2"
    const val INDIVIDUAL_PROJECT_TYPE = "ES"
    const val INDUCTION_PROJECT_TYPE = "PI"
  }

  @Test
  fun `service is registered when the feature is enabled`() {
    assertThat(ctx.getBeanProvider(AutomatedTravelTimeService::class.java).ifAvailable).isNotNull
  }

  @Nested
  inner class RecordingOutcomes {

    // project details are cached, so use a unique project per test
    private val projectCode = String.random(8)

    private lateinit var nonAttendedOutcomeCode: String

    @BeforeEach
    fun setUp() {
      nonAttendedOutcomeCode = contactOutcomeEntityRepository.findAll()
        .first { !it.attended && !it.enforceable }
        .code

      CommunityPaybackAndDeliusMockServer.setupPutAppointmentResponse(
        projectCode = projectCode,
        appointmentId = DELIUS_APPOINTMENT_ID,
      )
      CommunityPaybackAndDeliusMockServer.setupPostAdjustmentResponse(username = USERNAME, adjustmentId = DELIUS_ADJUSTMENT_ID)
      CommunityPaybackAndDeliusMockServer.setupGetAdjustmentsResponse(CRN, EVENT_NUMBER, emptyList())
      CommunityPaybackAndDeliusMockServer.setupGetAdjustmentResponse(adjustment = NDAdjustment.valid())
    }

    @ParameterizedTest
    @CsvSource(GROUP_PROJECT_TYPE, INDIVIDUAL_PROJECT_TYPE)
    fun `attended outcome for a London group or individual project creates a 60 minute travel time adjustment`(projectTypeCode: String) {
      setupAppointment(projectTypeCode = projectTypeCode)

      updateAppointment(outcomeCode = CODE_ATTENDED_COMPLIED)

      val appointment = appointmentEntity()
      val adjustment = adjustmentEntityRepository.findAll().single()
      CommunityPaybackAndDeliusMockServer.verifyPostAdjustment(
        username = USERNAME,
        expectedAdjustment = NDAdjustmentRequest(
          crn = CRN,
          eventNumber = EVENT_NUMBER,
          type = NDAdjustmentType.NEGATIVE,
          date = LocalDate.now(),
          reason = "TTX",
          minutes = 60,
          reference = adjustment.id,
        ),
      )
      assertThat(adjustment.deliusId).isEqualTo(DELIUS_ADJUSTMENT_ID)
      assertThat(adjustment.crn).isEqualTo(CRN)
      assertThat(adjustment.deliusEventNumber).isEqualTo(EVENT_NUMBER)
      assertThat(adjustment.type).isEqualTo(AdjustmentType.NEGATIVE)
      assertThat(adjustment.minutes).isEqualTo(60)
      assertThat(adjustment.date).isEqualTo(LocalDate.now())
      assertThat(adjustment.reason.deliusCode).isEqualTo("TTX")
      assertThat(adjustment.appointment?.id).isEqualTo(appointment.id)

      val createEvent = automatedCreateEvents(appointment).single()
      assertThat(createEvent.id).isEqualTo(adjustment.id)
      assertThat(createEvent.triggerType).isEqualTo(AdjustmentEventTriggerType.AUTOMATED_TRAVEL_TIME)

      domainEventAsserter.assertEventCount("community-payback.adjustment.created", 1)
    }

    @Test
    fun `no adjustment for providers that are not configured`() {
      setupAppointment(providerCode = "N03")

      updateAppointment(outcomeCode = CODE_ATTENDED_COMPLIED)

      CommunityPaybackAndDeliusMockServer.verifyPostAdjustment(username = USERNAME, count = 0)
      assertThat(adjustmentEventEntityRepository.findAll()).isEmpty()
    }

    @Test
    fun `no adjustment for project types that do not support travel time`() {
      setupAppointment(projectTypeCode = INDUCTION_PROJECT_TYPE)

      updateAppointment(outcomeCode = CODE_ATTENDED_COMPLIED)

      CommunityPaybackAndDeliusMockServer.verifyPostAdjustment(username = USERNAME, count = 0)
      assertThat(adjustmentEventEntityRepository.findAll()).isEmpty()
    }

    @Test
    fun `no adjustment for a non-attended outcome`() {
      setupAppointment()

      updateAppointment(outcomeCode = nonAttendedOutcomeCode)

      CommunityPaybackAndDeliusMockServer.verifyPostAdjustment(username = USERNAME, count = 0)
      assertThat(adjustmentEventEntityRepository.findAll()).isEmpty()
    }

    @Test
    fun `subsequent attended updates do not create another adjustment`() {
      setupAppointment()

      updateAppointment(outcomeCode = CODE_ATTENDED_COMPLIED, notes = "first")
      updateAppointment(outcomeCode = CODE_ATTENDED_COMPLIED, notes = "second")

      CommunityPaybackAndDeliusMockServer.verifyPostAdjustment(username = USERNAME, count = 1)
      assertThat(automatedCreateEvents(appointmentEntity())).hasSize(1)
      domainEventAsserter.assertEventCount("community-payback.adjustment.created", 1)
    }

    @Test
    fun `changing the outcome to non-attended deletes the adjustment`() {
      setupAppointment()
      updateAppointment(outcomeCode = CODE_ATTENDED_COMPLIED)
      val createEvent = automatedCreateEvents(appointmentEntity()).single()

      updateAppointment(outcomeCode = nonAttendedOutcomeCode)

      // the first DELETE is the orphan clean-up performed before creating the adjustment
      CommunityPaybackAndDeliusMockServer.verifyDeleteAdjustment(reference = createEvent.id, count = 2)
      val deleteEvent = deleteEvents().single()
      assertThat(deleteEvent.referencedEvent?.id).isEqualTo(createEvent.id)
      assertThat(deleteEvent.triggerType).isEqualTo(AdjustmentEventTriggerType.AUTOMATED_TRAVEL_TIME)
      assertThat(deleteEvent.triggeredBy).isEqualTo(USERNAME)
      assertThat(automatedCreateEvents(appointmentEntity())).isEmpty()
      domainEventAsserter.assertEventCount("community-payback.adjustment.deleted", 1)
    }

    @Test
    fun `further non-attended updates do not attempt another delete`() {
      setupAppointment()
      updateAppointment(outcomeCode = CODE_ATTENDED_COMPLIED)
      updateAppointment(outcomeCode = nonAttendedOutcomeCode)

      updateAppointment(outcomeCode = nonAttendedOutcomeCode, notes = "still absent")

      assertThat(deleteEvents()).hasSize(1)
      domainEventAsserter.assertEventCount("community-payback.adjustment.deleted", 1)
    }

    @Test
    fun `attendance can be removed and re-recorded repeatedly, keeping travel time in step`() {
      setupAppointment()

      updateAppointment(outcomeCode = CODE_ATTENDED_COMPLIED)
      val first = automatedCreateEvents(appointmentEntity()).single()
      updateAppointment(outcomeCode = nonAttendedOutcomeCode)

      CommunityPaybackAndDeliusMockServer.setupPostAdjustmentResponse(username = USERNAME, adjustmentId = DELIUS_ADJUSTMENT_ID + 1)
      updateAppointment(outcomeCode = CODE_ATTENDED_COMPLIED)
      val recreated = automatedCreateEvents(appointmentEntity()).single()
      assertThat(first.id).isEqualTo(expectedAutomatedAdjustmentId(previousCreations = 0))
      assertThat(recreated.id).isEqualTo(expectedAutomatedAdjustmentId(previousCreations = 1))
      assertThat(recreated.id).isNotEqualTo(first.id)
      assertThat(recreated.deliusAdjustmentId).isEqualTo(DELIUS_ADJUSTMENT_ID + 1)
      assertThat(adjustmentEntityRepository.count()).isEqualTo(2)

      CommunityPaybackAndDeliusMockServer.verifyPostAdjustment(username = USERNAME, count = 2)
      assertThat(automatedCreateEvents(appointmentEntity())).hasSize(1)

      updateAppointment(outcomeCode = nonAttendedOutcomeCode)

      assertThat(automatedCreateEvents(appointmentEntity())).isEmpty()
      assertThat(deleteEvents().map { it.referencedEvent?.id }).containsExactlyInAnyOrder(first.id, recreated.id)
      domainEventAsserter.assertEventCount("community-payback.adjustment.deleted", 2)
    }

    @Test
    fun `retrying after a failed attempt reuses the same id, so any orphaned adjustment is cleaned up`() {
      setupAppointment()
      val expectedId = expectedAutomatedAdjustmentId(previousCreations = 0)

      // the adjustment is created in NDelius, but the request then fails
      CommunityPaybackAndDeliusMockServer.setupGetAdjustment404Response(reference = expectedId)
      updateAppointment(outcomeCode = CODE_ATTENDED_COMPLIED, expectedStatus = { it.is5xxServerError })

      assertThat(adjustmentEventEntityRepository.findAll()).isEmpty()
      assertThat(adjustmentEntityRepository.findAll()).isEmpty()

      CommunityPaybackAndDeliusMockServer.setupGetAdjustmentResponse(reference = expectedId, adjustment = NDAdjustment.valid())
      updateAppointment(outcomeCode = CODE_ATTENDED_COMPLIED)

      // both attempts post the same reference
      CommunityPaybackAndDeliusMockServer.verifyPostAdjustment(
        username = USERNAME,
        count = 2,
        expectedAdjustment = NDAdjustmentRequest(
          crn = CRN,
          eventNumber = EVENT_NUMBER,
          type = NDAdjustmentType.NEGATIVE,
          date = LocalDate.now(),
          reason = "TTX",
          minutes = 60,
          reference = expectedId,
        ),
      )
      // orphan clean up before each attempt, plus the NDelius rollback when the first request failed
      CommunityPaybackAndDeliusMockServer.verifyDeleteAdjustment(reference = expectedId, count = 3)
      assertThat(automatedCreateEvents(appointmentEntity()).map { it.id }).containsExactly(expectedId)
      domainEventAsserter.assertEventCount("community-payback.adjustment.created", 1)
    }

    @Test
    fun `a manually created adjustment for the appointment is not deleted when attendance is removed`() {
      setupAppointment()
      val appointment = AppointmentEntity.valid().copy(
        deliusId = DELIUS_APPOINTMENT_ID,
        crn = CRN,
        deliusEventNumber = EVENT_NUMBER,
        projectCode = projectCode,
        providerCode = LONDON,
      ).persist(ctx)
      val manualCreateEvent = adjustmentEventEntityRepository.save(
        AdjustmentEventEntity.valid().copy(
          eventType = AdjustmentEventType.CREATE,
          triggerType = AdjustmentEventTriggerType.APPOINTMENT_TASK,
          appointment = appointment,
          adjustmentReason = adjustmentReasonEntityRepository.findByDeliusCode("TTX")!!,
        ),
      )

      updateAppointment(outcomeCode = nonAttendedOutcomeCode)

      CommunityPaybackAndDeliusMockServer.verifyDeleteAdjustment(reference = manualCreateEvent.id, count = 0)
      assertThat(deleteEvents()).isEmpty()
    }

    @Test
    fun `insufficient remaining requirement time for the travel time rejects the update`() {
      // the 30 minutes credited by the appointment fits, but the 60 minute travel time adjustment doesn't
      setupAppointment(requiredMinutes = 45)

      updateAppointment(
        outcomeCode = CODE_ATTENDED_COMPLIED,
        endTime = LocalTime.of(0, 30),
        expectedStatus = { it.isBadRequest },
      )

      CommunityPaybackAndDeliusMockServer.verifyPostAdjustment(username = USERNAME, count = 0)
      assertThat(adjustmentEventEntityRepository.findAll()).isEmpty()
    }

    @Test
    fun `failure deleting the adjustment upstream fails the update and keeps the adjustment`() {
      setupAppointment()
      updateAppointment(outcomeCode = CODE_ATTENDED_COMPLIED)
      val createEvent = automatedCreateEvents(appointmentEntity()).single()

      CommunityPaybackAndDeliusMockServer.setupDeleteAdjustmentResponse(reference = createEvent.id, status = 500)

      updateAppointment(outcomeCode = nonAttendedOutcomeCode, expectedStatus = { it.is5xxServerError })

      assertThat(automatedCreateEvents(appointmentEntity()).map { it.id }).containsExactly(createEvent.id)
      assertThat(deleteEvents()).isEmpty()
    }

    private fun setupAppointment(
      providerCode: String = LONDON,
      projectTypeCode: String = GROUP_PROJECT_TYPE,
      requiredMinutes: Long = 6000,
    ) {
      CommunityPaybackAndDeliusMockServer.Aggregates.setupGetDataMocksForUpdateAppointment(
        existingAppointment = NDAppointment.validNoOutcome(ctx).copy(
          id = DELIUS_APPOINTMENT_ID,
          project = NDProjectAndLocation.valid().copy(code = projectCode),
          date = LocalDate.now(),
          event = NDEvent.valid().copy(number = EVENT_NUMBER),
          case = NDCaseSummary.valid().copy(crn = CRN),
          sensitive = false,
        ),
        username = USERNAME,
        project = NDProject.valid().copy(
          code = projectCode,
          type = NDProjectType.valid().copy(code = projectTypeCode),
          provider = NDNameCode(name = "Provider $providerCode", code = providerCode),
          actualEndDateExclusive = null,
        ),
      )

      // replace the random upw details so remaining requirement time is predictable
      val upwDetails = NDUpwDetails.valid().copy(
        eventNumber = EVENT_NUMBER,
        sentenceDate = LocalDate.now().minusYears(1),
        requiredMinutes = requiredMinutes,
        completedMinutes = 0,
        adjustments = 0,
      )
      listOf(null, USERNAME).forEach { username ->
        CommunityPaybackAndDeliusMockServer.setupGetUpwDetailsSummaryResponse(
          crn = CRN,
          case = NDCaseSummary.valid().copy(crn = CRN),
          unpaidWorkDetails = listOf(upwDetails),
          username = username,
        )
      }
    }

    private fun updateAppointment(
      outcomeCode: String,
      notes: String = "notes",
      endTime: LocalTime = LocalTime.of(1, 0),
      expectedStatus: (StatusAssertions) -> WebTestClient.ResponseSpec = { it.isOk },
    ) {
      webTestClient.put()
        .uri("/admin/projects/$projectCode/appointments/$DELIUS_APPOINTMENT_ID")
        .addAdminUiAuthHeader(USERNAME)
        .bodyValue(
          UpdateAppointmentDto.valid(ctx).copy(
            deliusId = DELIUS_APPOINTMENT_ID,
            deliusVersionToUpdate = UUID.randomUUID(),
            date = LocalDate.now(),
            startTime = LocalTime.of(0, 0),
            endTime = endTime,
            contactOutcomeCode = outcomeCode,
            attendanceData = AttendanceDataDto.valid(),
            notes = notes,
            sensitive = false,
          ),
        )
        .exchange()
        .expectStatus()
        .let(expectedStatus)
    }

    private fun appointmentEntity() = appointmentEntityRepository.findByDeliusId(DELIUS_APPOINTMENT_ID)!!

    private fun deleteEvents() = adjustmentEventEntityRepository.findAll().filter { it.eventType == AdjustmentEventType.DELETE }
  }

  private fun expectedAutomatedAdjustmentId(previousCreations: Long) = DefaultAdjustmentIdGenerator().generateId(
    AutomatedAdjustmentProperties(
      appointmentDeliusId = DELIUS_APPOINTMENT_ID,
      triggerType = AdjustmentEventTriggerType.AUTOMATED_TRAVEL_TIME.name,
      previousCreations = previousCreations,
    ),
  )

  private fun automatedCreateEvents(appointment: AppointmentEntity) = adjustmentEventEntityRepository.findNondeletedCreateEvents(
    appointment = appointment,
    triggerType = AdjustmentEventTriggerType.AUTOMATED_TRAVEL_TIME,
  )
}
