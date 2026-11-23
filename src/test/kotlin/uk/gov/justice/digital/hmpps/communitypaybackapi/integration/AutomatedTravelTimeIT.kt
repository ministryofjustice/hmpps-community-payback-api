package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import com.github.tomakehurst.wiremock.client.WireMock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDNameCode
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProjectAndLocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProjectType
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDUpwDetails
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity.Companion.CODE_ATTENDED_COMPLIED
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.validNoOutcome
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.DomainEventAsserter
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.CommunityPaybackAndDeliusMockServer
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@TestPropertySource(
  properties = [
    "adjustments.automated-travel-time.enabled=true",
    "adjustments.automated-travel-time.provider-codes=N07,N56",
  ],
)
class AutomatedTravelTimeIT : IntegrationTestBase() {

  @Autowired
  lateinit var adjustmentEntityRepository: AdjustmentEntityRepository

  @Autowired
  lateinit var adjustmentEventEntityRepository: AdjustmentEventEntityRepository

  @Autowired
  lateinit var appointmentEntityRepository: AppointmentEntityRepository

  @Autowired
  lateinit var appointmentEventEntityRepository: AppointmentEventEntityRepository

  @Autowired
  lateinit var domainEventAsserter: DomainEventAsserter

  @Autowired
  lateinit var adjustmentReasonEntityRepository: AdjustmentReasonEntityRepository

  private val date = LocalDate.now().minusDays(1)
  private lateinit var project: NDProject
  private lateinit var existingAppointment: NDAppointment

  companion object {
    private const val USERNAME = "travel-time-user"
    private const val CRN = "X123456"
    private const val EVENT_NUMBER = 92
    private const val PROJECT_CODE = "TRAVEL01"
    private const val APPOINTMENT_ID = 1234L
    private const val ABSENT = "AAME"
  }

  @BeforeEach
  fun setup() {
    project = NDProject.valid(ctx).copy(
      code = PROJECT_CODE,
      provider = NDNameCode("Provider", "N07"),
      type = NDProjectType.valid().copy(code = "NP2"),
      actualEndDateExclusive = null,
    )
    existingAppointment = NDAppointment.validNoOutcome(ctx).copy(
      id = APPOINTMENT_ID,
      project = NDProjectAndLocation.valid().copy(code = PROJECT_CODE),
      projectType = project.type,
      date = date,
      event = NDEvent.valid().copy(number = EVENT_NUMBER),
      case = NDCaseSummary.valid().copy(crn = CRN),
      pickUpData = null,
      minutesCredited = 0,
    )
    stubAppointmentData()
    stubRemainingMinutes(1000)
    CommunityPaybackAndDeliusMockServer.setupGetAdjustmentsResponse(CRN, EVENT_NUMBER, emptyList())
    CommunityPaybackAndDeliusMockServer.setupPostAppointmentsResponse(PROJECT_CODE, 1)
    CommunityPaybackAndDeliusMockServer.setupPutAppointmentResponse(PROJECT_CODE, APPOINTMENT_ID)
    CommunityPaybackAndDeliusMockServer.setupPostAdjustmentResponse(USERNAME, adjustmentId = 25L)
  }

  @ParameterizedTest
  @CsvSource("N07, NP2", "N56, ICP")
  fun `creating an attended appointment persists and publishes a one hour adjustment`(provider: String, projectType: String) {
    project = project.copy(provider = NDNameCode("Provider", provider), type = project.type.copy(code = projectType))
    stubAppointmentData()
    stubRemainingMinutes(1000)

    createAppointment()

    val adjustment = adjustmentEntityRepository.findAll().single()
    val appointment = appointmentEntityRepository.findAll().single()
    assertThat(adjustment.deliusId).isEqualTo(25L)
    assertThat(adjustment.crn).isEqualTo(CRN)
    assertThat(adjustment.deliusEventNumber).isEqualTo(EVENT_NUMBER)
    assertThat(adjustment.type).isEqualTo(AdjustmentType.POSITIVE)
    assertThat(adjustment.minutes).isEqualTo(60)
    assertThat(adjustment.date).isEqualTo(date)
    assertThat(adjustment.reason.deliusCode).isEqualTo("TTX")
    assertThat(adjustment.appointment?.id).isEqualTo(appointment.id)

    val event = adjustmentEventEntityRepository.findAll().single()
    assertThat(event.id).isEqualTo(adjustment.id)
    assertThat(event.adjustment?.id).isEqualTo(adjustment.id)
    assertThat(event.appointment?.id).isEqualTo(appointment.id)
    assertThat(event.eventType).isEqualTo(AdjustmentEventType.CREATE)
    assertThat(event.triggerType).isEqualTo(AdjustmentEventTriggerType.AUTOMATED_TRAVEL_TIME)
    assertThat(event.triggeredBy).isEqualTo(USERNAME)

    WireMock.verify(
      1,
      WireMock.postRequestedFor(WireMock.urlEqualTo("/community-payback-and-delius/adjustments?username=$USERNAME"))
        .withRequestBody(
          WireMock.equalToJson(
            """[{"crn":"$CRN","eventNumber":$EVENT_NUMBER,"type":"POSITIVE","date":"$date","reason":"TTX","minutes":60,"reference":"${adjustment.id}"}]""",
          ),
        ),
    )
    domainEventAsserter.assertEventCount("community-payback.adjustment.created", 1)
    domainEventAsserter.assertEventCount("community-payback.appointment.created", 1)
  }

  @ParameterizedTest
  @CsvSource("N99, NP2, ATTC", "N07, PI, ATTC", "N07, NP2, AAME")
  fun `ineligible appointments do not create adjustments`(provider: String, projectType: String, outcome: String) {
    project = project.copy(provider = NDNameCode("Provider", provider), type = project.type.copy(code = projectType))
    stubAppointmentData()
    stubRemainingMinutes(1000)

    createAppointment(outcome)

    assertThat(appointmentEntityRepository.count()).isEqualTo(1)
    assertNoAdjustments()
  }

  @Test
  fun `updating to attendance creates an adjustment and further attended updates do not duplicate it`() {
    updateAppointment(CODE_ATTENDED_COMPLIED)
    val id = adjustmentEntityRepository.findAll().single().id

    // Change notes to ensure a second update is processed rather than short circuited as identical.
    updateAppointment(CODE_ATTENDED_COMPLIED, notes = "Updated attendance notes")

    assertThat(adjustmentEntityRepository.findAll().map { it.id }).containsExactly(id)
    assertThat(adjustmentEventEntityRepository.count()).isEqualTo(1)
    CommunityPaybackAndDeliusMockServer.verifyPostAdjustment(USERNAME, count = 1)
  }

  @Test
  fun `removing attendance deletes the automated adjustment once and attendance can create a new adjustment`() {
    updateAppointment(CODE_ATTENDED_COMPLIED)
    val first = adjustmentEventEntityRepository.findAll().single()
    CommunityPaybackAndDeliusMockServer.setupDeleteAdjustmentResponse(first.id)

    updateAppointment(ABSENT)
    updateAppointment(ABSENT, notes = "Updated absence notes")

    val deletion = adjustmentEventEntityRepository.findAll().single { it.eventType == AdjustmentEventType.DELETE }
    assertThat(deletion.triggerType).isEqualTo(AdjustmentEventTriggerType.AUTOMATED_TRAVEL_TIME)
    assertThat(deletion.referencedEvent?.id).isEqualTo(first.id)
    assertThat(deletion.adjustment?.id).isEqualTo(first.id)
    CommunityPaybackAndDeliusMockServer.verifyDeleteAdjustment(first.id, count = 1)
    domainEventAsserter.assertEventCount("community-payback.adjustment.deleted", 1)

    CommunityPaybackAndDeliusMockServer.setupPostAdjustmentResponse(USERNAME, adjustmentId = 26L)
    updateAppointment(CODE_ATTENDED_COMPLIED)

    val creates = adjustmentEventEntityRepository.findAll().filter { it.eventType == AdjustmentEventType.CREATE }
    assertThat(creates).hasSize(2)
    assertThat(creates.map { it.id }).doesNotHaveDuplicates()
    assertThat(adjustmentEntityRepository.count()).isEqualTo(2)
    CommunityPaybackAndDeliusMockServer.verifyPostAdjustment(USERNAME, count = 2)
    domainEventAsserter.assertEventCount("community-payback.adjustment.created", 2)
  }

  @Test
  fun `upstream 404 closes the attendance lifecycle and restored attendance credits a fresh hour`() {
    updateAppointment(CODE_ATTENDED_COMPLIED)
    val first = adjustmentEventEntityRepository.findAll().single()
    WireMock.stubFor(
      WireMock.delete("/community-payback-and-delius/adjustments/${first.id}")
        .willReturn(WireMock.aResponse().withStatus(404)),
    )

    updateAppointment(ABSENT)

    val deletion = adjustmentEventEntityRepository.findAll().single { it.eventType == AdjustmentEventType.DELETE }
    assertThat(deletion.referencedEvent?.id).isEqualTo(first.id)
    assertThat(deletion.triggerType).isEqualTo(AdjustmentEventTriggerType.AUTOMATED_TRAVEL_TIME)

    CommunityPaybackAndDeliusMockServer.setupPostAdjustmentResponse(USERNAME, adjustmentId = 26L)
    updateAppointment(CODE_ATTENDED_COMPLIED)

    val creates = adjustmentEventEntityRepository.findAll().filter { it.eventType == AdjustmentEventType.CREATE }
    assertThat(creates).hasSize(2)
    assertThat(creates.map { it.id }).doesNotHaveDuplicates()
    CommunityPaybackAndDeliusMockServer.verifyPostAdjustment(USERNAME, count = 2)
  }

  @Test
  fun `moving a future appointment into the past with attendance credits the updated date`() {
    existingAppointment = existingAppointment.copy(date = LocalDate.now().plusDays(1))
    stubAppointmentData()
    stubRemainingMinutes(1000)

    updateAppointment(CODE_ATTENDED_COMPLIED)

    assertThat(adjustmentEntityRepository.findAll().single().date).isEqualTo(date)
    assertThat(appointmentEntityRepository.findAll().single().date).isEqualTo(date)
  }

  @Test
  fun `clearing the outcome removes an existing automated adjustment`() {
    updateAppointment(CODE_ATTENDED_COMPLIED)
    val create = adjustmentEventEntityRepository.findAll().single()
    CommunityPaybackAndDeliusMockServer.setupDeleteAdjustmentResponse(create.id)

    // An appointment without an outcome must be scheduled in the future.
    updateAppointment(null, appointmentDate = LocalDate.now().plusDays(1))

    CommunityPaybackAndDeliusMockServer.verifyDeleteAdjustment(create.id, count = 1)
    assertThat(adjustmentEventEntityRepository.findAll().single { it.eventType == AdjustmentEventType.DELETE }.referencedEvent?.id)
      .isEqualTo(create.id)
  }

  @Test
  fun `an automated adjustment for another appointment does not prevent creation`() {
    val other = adjustmentEventEntityRepository.save(
      AdjustmentEventEntity.valid(ctx).copy(
        eventType = AdjustmentEventType.CREATE,
        triggerType = AdjustmentEventTriggerType.AUTOMATED_TRAVEL_TIME,
      ),
    )

    updateAppointment(CODE_ATTENDED_COMPLIED)

    val created = adjustmentEventEntityRepository.findAll().single { it.id != other.id }
    assertThat(created.appointment?.id).isNotEqualTo(other.appointment?.id)
    assertThat(created.triggerType).isEqualTo(AdjustmentEventTriggerType.AUTOMATED_TRAVEL_TIME)
    CommunityPaybackAndDeliusMockServer.verifyPostAdjustment(USERNAME, count = 1)
  }

  @Test
  fun `manual adjustment does not prevent automated creation and is not deleted on absence`() {
    updateAppointment(ABSENT)
    val appointment = appointmentEntityRepository.findAll().single()
    val manual = adjustmentEventEntityRepository.save(
      AdjustmentEventEntity.valid().copy(
        eventType = AdjustmentEventType.CREATE,
        triggerType = AdjustmentEventTriggerType.APPOINTMENT_TASK,
        appointment = appointment,
        adjustmentReason = requireNotNull(adjustmentReasonEntityRepository.findByDeliusCode("TTX")),
      ),
    )

    updateAppointment(CODE_ATTENDED_COMPLIED)
    val automated = adjustmentEventEntityRepository.findAll().single { it.triggerType == AdjustmentEventTriggerType.AUTOMATED_TRAVEL_TIME }
    CommunityPaybackAndDeliusMockServer.setupDeleteAdjustmentResponse(automated.id)
    updateAppointment(ABSENT)

    CommunityPaybackAndDeliusMockServer.verifyDeleteAdjustment(automated.id, count = 1)
    CommunityPaybackAndDeliusMockServer.verifyDeleteAdjustment(manual.id, count = 0)
    assertThat(adjustmentEventEntityRepository.findAll().single { it.eventType == AdjustmentEventType.DELETE }.referencedEvent?.id)
      .isEqualTo(automated.id)
  }

  @Test
  fun `insufficient remaining time skips adjustment while saving attendance`() {
    // The appointment credits 30 minutes, leaving too little allowance for the 60 minute adjustment.
    stubRemainingMinutes(59)

    updateAppointment(CODE_ATTENDED_COMPLIED)

    assertThat(appointmentEventEntityRepository.count()).isEqualTo(1)
    assertNoAdjustments()
    domainEventAsserter.assertEventCount("community-payback.appointment.updated", 1)
  }

  @Test
  fun `database failure after adjustment creation compensates upstream and rolls back local changes`() {
    // Delius returns ID 25 for the new adjustment. An existing row with that ID
    // causes PostgreSQL's unique constraint to reject the transaction.
    val existing = adjustmentEntityRepository.saveAndFlush(
      AdjustmentEntity(
        id = UUID.randomUUID(),
        deliusId = 25L,
        crn = CRN,
        deliusEventNumber = EVENT_NUMBER,
        type = AdjustmentType.POSITIVE,
        minutes = 60,
        date = date,
        reason = requireNotNull(adjustmentReasonEntityRepository.findByDeliusCode("TTX")),
        appointment = null,
      ),
    )
    WireMock.stubFor(
      WireMock.delete(WireMock.urlMatching("/community-payback-and-delius/adjustments/.*"))
        .willReturn(WireMock.aResponse().withStatus(204)),
    )

    updateAppointment(CODE_ATTENDED_COMPLIED, expectedStatus = 500)

    CommunityPaybackAndDeliusMockServer.verifyPostAdjustment(USERNAME, count = 1)
    val deletion = WireMock.findAll(
      WireMock.deleteRequestedFor(WireMock.urlMatching("/community-payback-and-delius/adjustments/.*")),
    ).single()
    val adjustmentId = UUID.fromString(deletion.url.substringAfterLast('/'))
    assertThat(adjustmentId).isNotEqualTo(existing.id)
    WireMock.verify(
      1,
      WireMock.postRequestedFor(WireMock.urlEqualTo("/community-payback-and-delius/adjustments?username=$USERNAME"))
        .withRequestBody(WireMock.matchingJsonPath("$[0].reference", WireMock.equalTo(adjustmentId.toString()))),
    )
    assertThat(appointmentEntityRepository.count()).isZero()
    assertThat(appointmentEventEntityRepository.count()).isZero()
    assertThat(adjustmentEntityRepository.findAll()).containsExactly(existing)
    assertThat(adjustmentEventEntityRepository.count()).isZero()
    domainEventAsserter.assertEventCount("community-payback.adjustment.created", 0)
    domainEventAsserter.assertEventCount("community-payback.appointment.updated", 0)
  }

  @Test
  fun `failure deleting adjustment rolls back absence and allows a retry`() {
    updateAppointment(CODE_ATTENDED_COMPLIED)
    val create = adjustmentEventEntityRepository.findAll().single()
    WireMock.stubFor(
      WireMock.delete("/community-payback-and-delius/adjustments/${create.id}")
        .willReturn(WireMock.aResponse().withStatus(500)),
    )

    updateAppointment(ABSENT, expectedStatus = 500)

    assertThat(adjustmentEventEntityRepository.findAll()).containsExactly(create)
    assertThat(appointmentEventEntityRepository.count()).isEqualTo(1)
    domainEventAsserter.assertEventCount("community-payback.adjustment.deleted", 0)

    CommunityPaybackAndDeliusMockServer.setupDeleteAdjustmentResponse(create.id)
    updateAppointment(ABSENT)

    assertThat(adjustmentEventEntityRepository.findAll().map { it.eventType })
      .containsExactlyInAnyOrder(AdjustmentEventType.CREATE, AdjustmentEventType.DELETE)
    CommunityPaybackAndDeliusMockServer.verifyDeleteAdjustment(create.id, count = 2)
  }

  private fun stubAppointmentData() {
    CommunityPaybackAndDeliusMockServer.Aggregates.setupGetDataMocksForUpdateAppointment(existingAppointment, project, USERNAME)
  }

  private fun stubRemainingMinutes(minutes: Long) {
    val details = NDUpwDetails.valid(ctx).copy(
      eventNumber = EVENT_NUMBER,
      sentenceDate = date.minusYears(1),
      requiredMinutes = minutes,
      completedMinutes = 0,
      adjustments = 0,
    )
    // Validation fetches the summary without a username, while system adjustment validation uses one.
    listOf(null, USERNAME).forEach { username ->
      CommunityPaybackAndDeliusMockServer.setupGetUpwDetailsSummaryResponse(
        crn = CRN,
        case = NDCaseSummary.valid().copy(crn = CRN),
        unpaidWorkDetails = listOf(details),
        username = username,
      )
    }
  }

  private fun createAppointment(outcome: String = CODE_ATTENDED_COMPLIED) {
    webTestClient.post()
      .uri("/admin/appointments")
      .addAdminUiAuthHeader(USERNAME)
      .bodyValue(
        CreateAppointmentDto.valid().copy(
          crn = CRN,
          deliusEventNumber = EVENT_NUMBER,
          projectCode = PROJECT_CODE,
          date = date,
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(10, 30),
          pickUpLocationCode = null,
          pickUpTime = null,
          contactOutcomeCode = outcome,
          supervisorOfficerCode = "SUP001",
        ),
      )
      .exchange()
      .expectStatus().isCreated
  }

  private fun updateAppointment(
    outcome: String?,
    notes: String = "Attendance notes",
    expectedStatus: Int = 200,
    appointmentDate: LocalDate = date,
  ) {
    webTestClient.put()
      .uri("/admin/projects/$PROJECT_CODE/appointments/$APPOINTMENT_ID")
      .addAdminUiAuthHeader(USERNAME)
      .bodyValue(
        UpdateAppointmentDto.valid(ctx).copy(
          deliusId = APPOINTMENT_ID,
          deliusVersionToUpdate = existingAppointment.version,
          date = appointmentDate,
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(10, 30),
          contactOutcomeCode = outcome,
          supervisorOfficerCode = "SUP001",
          notes = notes,
        ),
      )
      .exchange()
      .expectStatus().isEqualTo(expectedStatus)
  }

  private fun assertNoAdjustments() {
    assertThat(adjustmentEntityRepository.count()).isZero()
    assertThat(adjustmentEventEntityRepository.count()).isZero()
    CommunityPaybackAndDeliusMockServer.verifyPostAdjustment(USERNAME, count = 0)
  }
}
