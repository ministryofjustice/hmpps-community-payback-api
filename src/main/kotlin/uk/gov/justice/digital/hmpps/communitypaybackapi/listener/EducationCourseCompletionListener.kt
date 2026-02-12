package uk.gov.justice.digital.hmpps.communitypaybackapi.listener

import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.messaging.MessageHeaders
import org.springframework.messaging.handler.annotation.Headers
import org.springframework.stereotype.Service
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.EteService

@Service
@ConditionalOnProperty(name = ["community-payback.education-course-integration.enabled"], havingValue = "true")
class EducationCourseCompletionListener(
  private val jsonMapper: JsonMapper,
  private val eteService: EteService,
  private val sqsListenerErrorHandler: SqsListenerErrorHandler,
) {
  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @SqsListener(
    "educationcoursecompletionevents",
    factory = "hmppsQueueContainerFactoryProxy",
    pollTimeoutSeconds = $$"${hmpps.sqs.pollTimeoutSeconds:}",
  )
  fun courseCompletion(
    messageString: String,
    @Headers headers: MessageHeaders,
  ) {
    log.debug("Have received education course course completion message '$messageString'")
    sqsListenerErrorHandler.withErrorHandler(headers) {
      val message = jsonMapper.readValue(messageString, EducationCourseCompletionMessage::class.java)
      eteService.handleEducationCourseCompletionMessage(message)
    }
  }
}
