package uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util

import org.awaitility.Awaitility.await
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import org.springframework.test.context.event.annotation.BeforeTestMethod
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.SentryService
import java.util.concurrent.TimeUnit

@Service
@Primary
class MockSentryService : SentryService {

  var log: Logger = LoggerFactory.getLogger(this::class.java)

  private val capturedExceptions = mutableListOf<Throwable>()

  override fun captureException(throwable: Throwable) {
    log.info("Sentry Exception Captured", throwable)
    capturedExceptions.add(throwable)
  }

  fun getRaisedException(): Throwable {
    await()
      .atMost(1, TimeUnit.SECONDS)
      .until { capturedExceptions.isNotEmpty() }

    return capturedExceptions[0]
  }

  @BeforeTestMethod
  fun beforeTestMethod() {
    reset()
  }

  private fun reset() {
    capturedExceptions.clear()
  }
}
