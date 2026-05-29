package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.internal

import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import io.sentry.Sentry
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.web.reactive.function.client.WebClientRequestException
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.SentryServiceImpl
import java.net.URI

class SentryServiceTest {

  private val sentryService = SentryServiceImpl()

  @BeforeEach
  fun setUp() {
    mockkStatic(Sentry::class)
    every { Sentry.captureException(any()) } returns io.sentry.protocol.SentryId()
    every { Sentry.withScope(any()) } answers {
      val callback = firstArg<io.sentry.ScopeCallback>()
      val scope = io.mockk.mockk<io.sentry.IScope>(relaxed = true)
      callback.run(scope)
    }
  }

  @AfterEach
  fun tearDown() {
    unmockkStatic(Sentry::class)
  }

  @Test
  fun `captureException should capture with context if the exception is a WebClientRequestException`() {
    val cause = RuntimeException("Connection refused")
    val webClientException = WebClientRequestException(
      cause,
      HttpMethod.GET,
      URI.create("http://localhost"),
      org.springframework.http.HttpHeaders(),
    )

    sentryService.captureException(webClientException)

    verify { Sentry.withScope(any()) }
    verify { Sentry.captureException(webClientException) }
  }

  @Test
  fun `captureException should capture the exception itself if it's not a WebClientRequestException`() {
    val exception = RuntimeException("Some error")

    sentryService.captureException(exception)

    verify { Sentry.captureException(exception) }
  }
}
