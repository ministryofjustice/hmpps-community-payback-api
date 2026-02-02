package uk.gov.justice.digital.hmpps.communitypaybackapi.listener

import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.messaging.MessageHeaders
import org.springframework.messaging.handler.annotation.Headers
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.EteService

@Service
@ConditionalOnProperty(name = ["community-payback.education-course-integration.enabled"], havingValue = "true")
class EducationCourseListener(
  private val objectMapper: ObjectMapper,
  private val eteService: EteService,
  private val sqsListenerErrorHandler: SqsListenerErrorHandler,
) {
  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @SqsListener(
    "educationcoursecompletionqueue",
    factory = "hmppsQueueContainerFactoryProxy",
    pollTimeoutSeconds = $$"${hmpps.sqs.pollTimeoutSeconds:}",
  )
  fun courseCompletion(
    messageString: String,
    @Headers headers: MessageHeaders,
  ) {
    log.debug("Have received education course course completion message '$messageString'")
    sqsListenerErrorHandler.withErrorHandler(headers) {
      val message = objectMapper.readValue(messageString, EducationCourseCompletionMessage::class.java)
      eteService.handleEducationCourseMessage(message)
    }
  }
}
