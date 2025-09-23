package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.util

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.nio.charset.Charset

object WebClientResponseExceptionFactory {
  fun notFound() = WebClientResponseException.create(
    HttpStatus.NOT_FOUND.value(),
    "Not Found",
    HttpHeaders(),
    "the body".toByteArray(Charset.forName("UTF-8")),
    null,
  )
}
