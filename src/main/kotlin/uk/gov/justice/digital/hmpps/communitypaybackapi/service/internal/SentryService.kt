package uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal

import io.sentry.Sentry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

interface SentryService {
  fun captureException(throwable: Throwable)
}

@Service
class SentryServiceImpl : SentryService {

  var log: Logger = LoggerFactory.getLogger(this::class.java)

  override fun captureException(throwable: Throwable) {
    log.debug("Will capture exception in sentry", throwable)
    Sentry.captureException(throwable)
  }
}
