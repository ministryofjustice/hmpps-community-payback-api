package uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util

import org.assertj.core.api.Assertions.assertThat
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

  override fun captureMessage(message: String) {
    // do nothing
  }

  fun getRaisedExceptions(): List<Throwable> {
    await()
      .atMost(1, TimeUnit.SECONDS)
      .until { capturedExceptions.isNotEmpty() }

    return capturedExceptions
  }

  fun assertExceptionRaisedWithMessage(regex: String) {
    await()
      .atMost(1, TimeUnit.SECONDS)
      .untilAsserted {
        assertThat(capturedExceptions)
          .anySatisfy {
            assertThat(it.message).matches(regex)
          }
      }
  }

  @BeforeTestMethod
  fun beforeTestMethod() {
    reset()
  }

  private fun reset() {
    capturedExceptions.clear()
  }
}
