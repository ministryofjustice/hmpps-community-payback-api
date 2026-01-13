package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.util

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.nio.charset.Charset

object WebClientResponseExceptionFactory {
  fun badRequest(body: String): WebClientResponseException = WebClientResponseException.create(
    HttpStatus.BAD_REQUEST.value(),
    "Bad Request",
    HttpHeaders(),
    body.toByteArray(Charset.forName("UTF-8")),
    null,
  )

  fun conflict(): WebClientResponseException = WebClientResponseException.create(
    HttpStatus.CONFLICT.value(),
    "Conflict",
    HttpHeaders(),
    "the body".toByteArray(Charset.forName("UTF-8")),
    null,
  )

  fun notFound(): WebClientResponseException = WebClientResponseException.create(
    HttpStatus.NOT_FOUND.value(),
    "Not Found",
    HttpHeaders(),
    "the body".toByteArray(Charset.forName("UTF-8")),
    null,
  )
}
