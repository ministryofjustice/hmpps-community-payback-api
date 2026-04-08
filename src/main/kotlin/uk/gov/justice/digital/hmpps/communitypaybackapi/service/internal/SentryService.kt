package uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal

import io.sentry.Sentry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

interface SentryService {
  fun captureException(throwable: Throwable)
  fun captureMessage(message: String)
}

@Service
class SentryServiceImpl : SentryService {

  var log: Logger = LoggerFactory.getLogger(this::class.java)

  override fun captureException(throwable: Throwable) {
    log.debug("Will capture exception in sentry", throwable)
    Sentry.captureException(throwable)
  }

  override fun captureMessage(message: String) {
    log.debug("Will capture message in sentry '{}'", message)
    Sentry.captureMessage(message)
  }
}
