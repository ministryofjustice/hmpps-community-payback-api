package uk.gov.justice.digital.hmpps.communitypaybackapi.listener

import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(name = ["community-payback.community-campus-integration.enabled"], havingValue = "true")
class CommunityCampusListener(
  private val objectMapper: ObjectMapper,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    var messageReceived: Boolean = false
  }

  @SqsListener("communitycampuscoursecompletionqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun courseCompletion(messageString: String) {
    log.info("Have community campus course completion message '$messageString'")

    objectMapper.readValue(messageString, CommunityCampusCourseCompletionMessage::class.java)
    messageReceived = true
  }
}
