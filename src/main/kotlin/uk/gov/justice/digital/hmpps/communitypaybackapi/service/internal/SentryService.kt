package uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal

import io.sentry.Sentry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientRequestException

interface SentryService {
  fun captureException(throwable: Throwable)
  fun captureMessage(message: String)
}

@Service
class SentryServiceImpl : SentryService {

  var log: Logger = LoggerFactory.getLogger(this::class.java)

  override fun captureException(throwable: Throwable) {
    log.debug("Will capture exception in sentry", throwable)
    when (throwable) {
      is WebClientRequestException -> captureWebClientRequestException(throwable)
      else -> Sentry.captureException(throwable)
    }
  }

  private fun captureWebClientRequestException(throwable: WebClientRequestException) {
    val rootCause = rootCauseOf(throwable)
    Sentry.withScope { scope ->
      scope.setTag("http.method", throwable.method?.name() ?: "unknown")
      scope.setTag("root_cause.type", rootCause.javaClass.simpleName)
      scope.setContexts(
        "webClientRequest",
        mapOf(
          "method" to (throwable.method?.name() ?: "unknown"),
          "uri" to throwable.uri?.toString(),
          "message" to throwable.message,
          "directCause" to throwable.cause?.javaClass?.name,
          "directCauseMessage" to throwable.cause?.message,
          "rootCause" to rootCause.javaClass.name,
          "rootCauseMessage" to rootCause.message,
        ),
      )
      Sentry.captureException(throwable)
    }
  }

  override fun captureMessage(message: String) {
    log.debug("Will capture message in sentry '{}'", message)
    Sentry.captureMessage(message)
  }

  private fun rootCauseOf(throwable: Throwable): Throwable {
    var rootCause = throwable
    while (rootCause.cause != null && rootCause.cause != rootCause) {
      rootCause = rootCause.cause!!
    }
    return rootCause
  }
}
