package uk.gov.justice.digital.hmpps.communitypaybackapi.listener

import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.EteService

@Service
@ConditionalOnProperty(name = ["community-payback.community-campus-integration.enabled"], havingValue = "true")
class CommunityCampusListener(
  private val objectMapper: ObjectMapper,
  private val eteService: EteService,
) {
  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @SqsListener("communitycampuscoursecompletionqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun courseCompletion(messageString: String) {
    log.debug("Have received community campus course completion message '$messageString'")

    val message = objectMapper.readValue(messageString, CommunityCampusCourseCompletionMessage::class.java)

    eteService.handleCommunityCampusMessage(message)
  }
}
