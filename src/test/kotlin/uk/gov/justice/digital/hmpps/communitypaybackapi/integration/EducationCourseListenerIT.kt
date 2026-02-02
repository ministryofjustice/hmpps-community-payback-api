package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCreatedAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.CommunityPaybackAndDeliusMockServer
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseCompletionMessage
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import java.util.concurrent.TimeUnit

class EducationCourseListenerIT : IntegrationTestBase() {

  @Autowired
  lateinit var hmppsQueueService: HmppsQueueService

  @Autowired
  lateinit var objectMapper: ObjectMapper

  @Autowired
  lateinit var eteCourseEventEntityRepository: EteCourseEventEntityRepository

  companion object {
    const val QUEUE_NAME = "educationcoursecompletionqueue"
  }

  @Nested
  inner class CourseCompletion {

    @BeforeEach
    fun before() {
      eteCourseEventEntityRepository.deleteAll()
    }

    @Test
    fun `Message is received`() {
      CommunityPaybackAndDeliusMockServer.postAppointments(
        projectCode = "N56CCTEST",
        response = listOf(
          NDCreatedAppointment(id = 1L),
        ),
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
        assertThat(eteCourseEventEntityRepository.count()).isEqualTo(1)
      }
    }
  }
}
