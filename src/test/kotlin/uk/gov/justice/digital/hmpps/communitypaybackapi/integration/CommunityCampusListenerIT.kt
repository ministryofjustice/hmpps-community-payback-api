package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.CommunityCampusCourseCompletionMessage
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.CommunityCampusListener
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException

class CommunityCampusListenerIT : IntegrationTestBase() {

  @Autowired
  lateinit var hmppsQueueService: HmppsQueueService

  @Autowired
  lateinit var objectMapper: ObjectMapper

  companion object {
    const val QUEUE_NAME = "communitycampuscoursecompletionqueue"
  }

  @Nested
  inner class CourseCompletion {

    @Test
    fun `Message is received`() {
      val message = objectMapper.writeValueAsString(CommunityCampusCourseCompletionMessage.valid())

      val queue = hmppsQueueService.findByQueueId(QUEUE_NAME)
        ?: throw MissingQueueException("HmppsQueue $QUEUE_NAME not found")

      queue.sqsClient.sendMessage(
        SendMessageRequest.builder()
          .queueUrl(queue.queueUrl)
          .messageBody(message)
          .build(),
      )

      Thread.sleep(1000)

      assertThat(CommunityCampusListener.messageReceived).isTrue()
    }
  }
}
