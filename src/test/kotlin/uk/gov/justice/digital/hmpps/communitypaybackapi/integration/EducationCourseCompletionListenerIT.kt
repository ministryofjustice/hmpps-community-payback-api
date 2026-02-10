package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.CommunityPaybackAndDeliusMockServer
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseCompletionMessage
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import java.util.concurrent.TimeUnit

class EducationCourseCompletionListenerIT : IntegrationTestBase() {

  @Autowired
  lateinit var hmppsQueueService: HmppsQueueService

  @Autowired
  lateinit var objectMapper: ObjectMapper

  @Autowired
  lateinit var eteCourseCompletionEventEntityRepository: EteCourseCompletionEventEntityRepository

  companion object {
    const val QUEUE_NAME = "educationcoursecompletionevents"
  }

  @Nested
  inner class CourseCompletion {

    @BeforeEach
    fun before() {
      eteCourseCompletionEventEntityRepository.deleteAll()
    }

    @Test
    fun `Message is received`() {
      CommunityPaybackAndDeliusMockServer.getProject(NDProject.valid(ctx).copy(code = "N56CCTEST"))
      CommunityPaybackAndDeliusMockServer.postAppointments(
        projectCode = "N56CCTEST",
        appointmentCount = 1,
      )

      val message = objectMapper.writeValueAsString(EducationCourseCompletionMessage.valid())
      val queue = hmppsQueueService.findByQueueId(QUEUE_NAME)
        ?: throw MissingQueueException("HmppsQueue $QUEUE_NAME not found")

      queue.sqsClient.sendMessage(
        SendMessageRequest.builder()
          .queueUrl(queue.queueUrl)
          .messageBody(message)
          .build(),
      )

      await().atMost(5, TimeUnit.SECONDS).untilAsserted {
        assertThat(eteCourseCompletionEventEntityRepository.count()).isEqualTo(1)
      }
    }
  }
}
