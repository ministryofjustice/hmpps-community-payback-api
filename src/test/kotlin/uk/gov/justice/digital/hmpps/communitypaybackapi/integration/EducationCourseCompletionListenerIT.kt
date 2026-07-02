package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProjectOutcomeStats
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.CommunityCampusPduEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionDraftResolutionRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.OfficeUpwTeamMappingEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.OfficeUpwTeamMappingRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.listener.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.DatabasePurgeUtils
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.CommunityPaybackAndDeliusMockServer
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.ProbationOffenderSearchMockServer
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseCompletionMessage
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseMessageAttributes
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import java.util.UUID
import java.util.concurrent.TimeUnit

class EducationCourseCompletionListenerIT : IntegrationTestBase() {

  @Autowired
  lateinit var hmppsQueueService: HmppsQueueService

  @Autowired
  lateinit var jsonMapper: JsonMapper

  @Autowired
  lateinit var eteCourseCompletionEventEntityRepository: EteCourseCompletionEventEntityRepository

  @Autowired
  lateinit var eteCourseCompletionDraftResolutionRepository: EteCourseCompletionDraftResolutionRepository

  @Autowired
  lateinit var communityCampusPduEntityRepository: CommunityCampusPduEntityRepository

  @Autowired
  lateinit var officeUpwTeamMappingRepository: OfficeUpwTeamMappingRepository

  @Autowired
  lateinit var databasePurgeUtils: DatabasePurgeUtils

  companion object {
    const val QUEUE_NAME = "educationcoursecompletionevents"
  }

  @Nested
  inner class CourseCompletion {

    @BeforeEach
    fun before() {
      databasePurgeUtils.deleteAllEteData()
      officeUpwTeamMappingRepository.deleteAll()
    }

    @Test
    fun `Message is received, pdu matching is case insensitive`() {
      ProbationOffenderSearchMockServer.stubNoMatches()
      val pdu = communityCampusPduEntityRepository.findByName("Cardiff and Vale")

      val message = jsonMapper.writeValueAsString(
        EducationCourseCompletionMessage.valid(ctx).copy(
          messageAttributes = EducationCourseMessageAttributes.valid().copy(
            pdu = "CARDIFF and VALE",
          ),
        ),
      )

      val queue = hmppsQueueService.findByQueueId(QUEUE_NAME)
        ?: throw MissingQueueException("HmppsQueue $QUEUE_NAME not found")

      queue.sqsClient.sendMessage(
        SendMessageRequest.builder()
          .queueUrl(queue.queueUrl)
          .messageBody(message)
          .build(),
      )

      await().atMost(10, TimeUnit.SECONDS).untilAsserted {
        assertThat(eteCourseCompletionEventEntityRepository.count()).isEqualTo(1)
      }

      assertThat(eteCourseCompletionEventEntityRepository.findAll()[0].pdu).isEqualTo(pdu)
    }

    @Test
    fun `CRN is populated in draft resolution when person search returns a single match`() {
      ProbationOffenderSearchMockServer.stubSingleMatch("X12345")

      sendMessage(EducationCourseCompletionMessage.valid(ctx))

      await().atMost(10, TimeUnit.SECONDS).untilAsserted {
        assertThat(eteCourseCompletionDraftResolutionRepository.count()).isEqualTo(1)
      }

      val draft = eteCourseCompletionDraftResolutionRepository.findAll().first()
      assertThat(draft.crn).isEqualTo("X12345")
      assertThat(draft.teamCode).isNull()
      assertThat(draft.projectCode).isNull()
      assertThat(draft.appointmentIdToUpdate).isNull()
    }

    @Test
    fun `team code is populated in draft resolution when office maps to a UPW team`() {
      ProbationOffenderSearchMockServer.stubNoMatches()

      val pdu = communityCampusPduEntityRepository.findByName("Hertfordshire")!!
      val office = "St Albans ${UUID.randomUUID()}"
      officeUpwTeamMappingRepository.save(
        OfficeUpwTeamMappingEntity(
          id = UUID.randomUUID(),
          pdu = pdu,
          office = office,
          teamCode = "N56ST",
        ),
      )
      CommunityPaybackAndDeliusMockServer.setupGetProjectsResponse(
        providerCode = pdu.providerCode,
        teamCode = "N56ST",
        projectTypeCodes = listOf("ET1", "ET3", "ET5", "UP06"),
        response = listOf(
          NDProjectOutcomeStats.valid().copy(
            project = NDProject.valid(ctx).copy(
              name = "ETE Portal - Health & Safety in Construction",
              code = "PROJECT1",
            ),
          ),
        ),
        pageSize = 500,
      )

      sendMessage(
        EducationCourseCompletionMessage.valid(ctx).copy(
          messageAttributes = EducationCourseMessageAttributes.valid(ctx).copy(
            region = "East of England",
            pdu = pdu.name,
            office = office,
            courseName = "Health and Safety in Construction",
          ),
        ),
      )

      await().atMost(10, TimeUnit.SECONDS).untilAsserted {
        assertThat(eteCourseCompletionDraftResolutionRepository.count()).isEqualTo(1)
      }

      val draft = eteCourseCompletionDraftResolutionRepository.findAll().first()
      assertThat(draft.crn).isNull()
      assertThat(draft.teamCode).isEqualTo("N56ST")
      assertThat(draft.projectCode).isEqualTo("PROJECT1")
      assertThat(draft.appointmentIdToUpdate).isNull()
    }

    @Test
    fun `CRN is null in draft resolution when person search returns no matches`() {
      ProbationOffenderSearchMockServer.stubNoMatches()

      sendMessage(EducationCourseCompletionMessage.valid(ctx))

      await().atMost(10, TimeUnit.SECONDS).untilAsserted {
        assertThat(eteCourseCompletionDraftResolutionRepository.count()).isEqualTo(1)
      }

      assertThat(eteCourseCompletionDraftResolutionRepository.findAll().first().crn).isNull()
    }

    @Test
    fun `CRN is null in draft resolution when person search returns multiple matches`() {
      ProbationOffenderSearchMockServer.stubMultipleMatches("X00001", "X00002")

      sendMessage(EducationCourseCompletionMessage.valid(ctx))

      await().atMost(10, TimeUnit.SECONDS).untilAsserted {
        assertThat(eteCourseCompletionDraftResolutionRepository.count()).isEqualTo(1)
      }

      assertThat(eteCourseCompletionDraftResolutionRepository.findAll().first().crn).isNull()
    }

    @Test
    fun `event is still persisted when person search fails`() {
      ProbationOffenderSearchMockServer.stubSearchError()

      sendMessage(EducationCourseCompletionMessage.valid(ctx))

      await().atMost(10, TimeUnit.SECONDS).untilAsserted {
        assertThat(eteCourseCompletionEventEntityRepository.count()).isEqualTo(1)
      }
    }

    private fun sendMessage(message: EducationCourseCompletionMessage) {
      val queue = hmppsQueueService.findByQueueId(QUEUE_NAME)
        ?: throw MissingQueueException("HmppsQueue $QUEUE_NAME not found")
      queue.sqsClient.sendMessage(
        SendMessageRequest.builder()
          .queueUrl(queue.queueUrl)
          .messageBody(jsonMapper.writeValueAsString(message))
          .build(),
      )
    }
  }
}
