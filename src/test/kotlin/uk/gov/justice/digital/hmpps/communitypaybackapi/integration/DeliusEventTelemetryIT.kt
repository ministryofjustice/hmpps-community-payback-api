package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDNameCode
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProjectAndLocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProjectType
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProvider
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDTeam
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentTaskEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ProjectTypeEntity.Companion.GROUP_PLACEMENT_NATIONAL_PROJECT_CODE
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.validNoOutcome
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.persist
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.listener.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.DatabasePurgeUtils
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.MockTelemetryService
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.CommunityPaybackAndDeliusMockServer
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseCompletionMessage
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseMessageAttributes
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentCreationService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentEventTrigger
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit

class DeliusEventTelemetryIT : IntegrationTestBase() {

  @Autowired
  lateinit var mockTelemetryService: MockTelemetryService

  @Autowired
  lateinit var appointmentCreationService: AppointmentCreationService

  @Autowired
  lateinit var jsonMapper: JsonMapper

  @Autowired
  lateinit var hmppsQueueService: HmppsQueueService

  @Autowired
  lateinit var eteCourseCompletionEventEntityRepository: EteCourseCompletionEventEntityRepository

  @Autowired
  lateinit var databasePurgeUtils: DatabasePurgeUtils

  companion object {
    const val QUEUE_NAME = "educationcoursecompletionevents"

    const val CRN = "X123456"
    const val EVENT_NUMBER = 1
    const val PROJECT_CODE = "PROJ001"
    const val PROJECT_NAME = "Test Project"
    const val PROVIDER_CODE = "P01"
    const val PROVIDER_NAME = "Provider 1"
    const val TEAM_CODE = "T01"
    const val TEAM_NAME = "Team 1"
  }

  @BeforeEach
  fun setUp() {
    mockTelemetryService.beforeTestMethod()
    databasePurgeUtils.deleteAllEteData()
  }

  @AfterEach
  fun tearDown() {
    eteCourseCompletionEventEntityRepository.deleteAll()
  }

  @Test
  fun `should track telemetry when an appointment is created`() {
    CommunityPaybackAndDeliusMockServer.Aggregates.setupGetDataMocksForCreateAppointment(
      crn = CRN,
      eventNumber = EVENT_NUMBER,
      project = NDProject.valid(ctx).copy(
        code = PROJECT_CODE,
        name = PROJECT_NAME,
        type = NDProjectType.valid().copy(code = GROUP_PLACEMENT_NATIONAL_PROJECT_CODE),
        provider = NDNameCode(name = PROVIDER_NAME, code = PROVIDER_CODE),
        team = NDNameCode(name = TEAM_NAME, code = TEAM_CODE),
        actualEndDateExclusive = null,
      ),
    )
    CommunityPaybackAndDeliusMockServer.setupPostAppointmentsResponse(PROJECT_CODE, 1)

    val outcome = ContactOutcomeEntity.valid().persist(ctx)
    val request = MockHttpServletRequest()
    RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))
    try {
      appointmentCreationService.createAppointment(
        CreateAppointmentDto.valid().copy(
          crn = CRN,
          projectCode = PROJECT_CODE,
          deliusEventNumber = EVENT_NUMBER,
          contactOutcomeCode = outcome.code,
          pickUpLocationCode = null,
          date = LocalDate.now(),
        ),
        AppointmentEventTrigger(
          triggeredAt = OffsetDateTime.now(),
          triggerType = AppointmentEventTriggerType.USER,
          triggeredBy = "theusername",
        ),
      )
    } finally {
      RequestContextHolder.resetRequestAttributes()
    }

    val events = mockTelemetryService.getEventsWithName("AppointmentEvent")
    assertThat(events).hasSize(1)
    val properties = events[0].properties
    assertThat(properties["crn"]).isEqualTo(CRN)
    assertThat(properties["deliusAppointmentId"]).isNotNull()
    assertThat(properties["projectType"]).isEqualTo("Group Placement - National Project")
    assertThat(properties["region"]).isEqualTo(PROVIDER_NAME)
    assertThat(properties["triggeredAt"]).isNotNull()
    assertThat(properties["triggeredBy"]).isEqualTo("theusername")
    assertThat(properties["eventType"]).isEqualTo("CREATED")
  }

  @Test
  fun `should track telemetry when an appointment is updated`() {
    CommunityPaybackAndDeliusMockServer.Aggregates.setupGetDataMocksForUpdateAppointment(
      existingAppointment = NDAppointment.validNoOutcome(ctx).copy(
        id = 1234L,
        project = NDProjectAndLocation.valid().copy(code = PROJECT_CODE, name = PROJECT_NAME),
        date = LocalDate.now(),
        event = NDEvent.valid().copy(number = EVENT_NUMBER),
        case = NDCaseSummary.valid().copy(crn = CRN),
        team = NDTeam(name = TEAM_NAME, code = TEAM_CODE),
        provider = NDProvider(name = PROVIDER_NAME, code = PROVIDER_CODE),
      ),
      username = "theusername",
      project = NDProject.valid(ctx).copy(
        code = PROJECT_CODE,
        name = PROJECT_NAME,
        type = NDProjectType.valid().copy(code = GROUP_PLACEMENT_NATIONAL_PROJECT_CODE),
        provider = NDNameCode(name = PROVIDER_NAME, code = PROVIDER_CODE),
        team = NDNameCode(name = TEAM_NAME, code = TEAM_CODE),
      ),
    )

    CommunityPaybackAndDeliusMockServer.setupPutAppointmentResponse(
      projectCode = PROJECT_CODE,
      appointmentId = 1234L,
    )

    webTestClient.put()
      .uri("/admin/projects/$PROJECT_CODE/appointments/1234")
      .addAdminUiAuthHeader("theusername")
      .bodyValue(
        UpdateAppointmentDto.valid(ctx).copy(
          deliusId = 1234L,
        ),
      )
      .exchange()
      .expectStatus()
      .isOk

    val events = mockTelemetryService.getEventsWithName("AppointmentEvent")
    assertThat(events).hasSize(1)
    val properties = events[0].properties
    assertThat(properties["crn"]).isEqualTo(CRN)
    assertThat(properties["deliusAppointmentId"]).isEqualTo("1234")
    assertThat(properties["projectType"]).isEqualTo("Group Placement - National Project")
    assertThat(properties["region"]).isEqualTo(PROVIDER_NAME)
    assertThat(properties["triggeredAt"]).isNotNull()
    assertThat(properties["triggeredBy"]).isEqualTo("theusername")
    assertThat(properties["eventType"]).isEqualTo("UPDATED")
  }

  @Test
  fun `should track telemetry when an adjustment is created`() {
    val appointment = AppointmentEntity.valid().copy(
      crn = CRN,
      deliusId = 1234L,
      providerCode = PROVIDER_CODE,
    ).persist(ctx)
    val task = AppointmentTaskEntity.valid().copy(appointment = appointment).persist(ctx)

    CommunityPaybackAndDeliusMockServer.setupGetUpwDetailsSummaryResponse(
      crn = CRN,
      username = "theusername",
      case = NDCaseSummary.valid(),
      unpaidWorkDetails = emptyList(),
    )
    CommunityPaybackAndDeliusMockServer.setupPostAdjustmentResponse(username = "theusername")

    webTestClient.post()
      .uri("/admin/offenders/$CRN/unpaid-work-details/$EVENT_NUMBER/adjustments")
      .addAdminUiAuthHeader("theusername")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(
        CreateAdjustmentDto.valid(ctx).copy(
          taskId = task.id,
        ),
      )
      .exchange()
      .expectStatus()
      .isOk

    val events = mockTelemetryService.getEventsWithName("AdjustmentEvent")
    assertThat(events).hasSize(1)
    val properties = events[0].properties
    assertThat(properties["crn"]).isEqualTo(CRN)
    assertThat(properties["deliusAppointmentId"]).isEqualTo("1234")
    assertThat(properties["deliusAdjustmentId"]).isEqualTo("1")
    assertThat(properties["providerCode"]).isEqualTo(PROVIDER_CODE)
    assertThat(properties["triggeredAt"]).isNotNull()
    assertThat(properties["triggeredBy"]).isEqualTo(task.id.toString())
    assertThat(properties["eventType"]).isEqualTo("CREATED")
  }

  @Test
  fun `should track telemetry when a course completion is received`() {
    val queue = hmppsQueueService.findByQueueId(QUEUE_NAME)
      ?: throw MissingQueueException("HmppsQueue $QUEUE_NAME not found")

    val attributes = EducationCourseMessageAttributes.valid(ctx).copy()
    val message = EducationCourseCompletionMessage.valid(ctx).copy(messageAttributes = attributes)

    queue.sqsClient.sendMessage(
      SendMessageRequest.builder()
        .messageBody(jsonMapper.writeValueAsString(message))
        .queueUrl(queue.queueUrl)
        .build(),
    )

    await().atMost(10, TimeUnit.SECONDS).untilAsserted {
      val events = mockTelemetryService.getEventsWithName("CourseCompletionEvent")
      assertThat(events).hasSize(1)
      val properties = events[0].properties

      assertThat(properties["attempts"]).isNotNull
      assertThat(properties["attempts"]).isEqualTo(attributes.attempts.toString())
      assertThat(properties["courseName"]).isEqualTo(attributes.courseName)
      assertThat(properties["courseType"]).isEqualTo(attributes.courseType)
      assertThat(properties["provider"]).isEqualTo(attributes.provider)
      assertThat(properties["region"]).isEqualTo(attributes.region)
      assertThat(properties["triggeredAt"]).isNotNull()
      assertThat(properties["triggeredBy"]).isEqualTo(attributes.externalReference)
      assertThat(properties["eventType"]).isEqualTo("RECEIVED")
    }
  }
}
