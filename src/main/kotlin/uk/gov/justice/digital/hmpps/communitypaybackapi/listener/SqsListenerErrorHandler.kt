package uk.gov.justice.digital.hmpps.communitypaybackapi.listener

import org.springframework.messaging.MessageHeaders
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.SentryService

@Service
class SqsListenerErrorHandler(
  val sentryService: SentryService,
) {

  @SuppressWarnings("TooGenericExceptionCaught")
  fun withErrorHandler(
    headers: MessageHeaders,
    action: () -> Unit,
  ) {
    try {
      action.invoke()
    } catch (t: Throwable) {
      val wrapper = SqsListenerException("Error occurred handling message with ID '${headers.id}' - ${t.message}", t)
      sentryService.captureException(wrapper)
      throw wrapper
    }
  }
}

class SqsListenerException : Exception {
  constructor(message: String, cause: Throwable) : super(message, cause)
}
