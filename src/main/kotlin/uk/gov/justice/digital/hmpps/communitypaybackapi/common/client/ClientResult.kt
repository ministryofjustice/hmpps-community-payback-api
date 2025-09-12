package uk.gov.justice.digital.hmpps.communitypaybackapi.common.client

import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.reactive.function.client.WebClientResponseException

sealed interface ClientResult<T> {
  data class Success<T>(
    val status: HttpStatus,
    val body: T,
  ) : ClientResult<T>

  sealed interface Failure<T> : ClientResult<T> {
    fun throwException(): Nothing

    data class HttpResponse<T>(
      val method: HttpMethod,
      val path: String,
      val status: HttpStatus,
      val body: String?,
    ) : Failure<T> {
      override fun throwException() = error("Unable to complete $method request to $path: $status")
    }

    data class Other<T>(
      val exception: Exception,
    ) : Failure<T> {
      override fun throwException() = throw exception
    }
  }
}

@SuppressWarnings("TooGenericExceptionCaught")
fun <T> callForClientResult(
  action: () -> ResponseEntity<T>,
): ClientResult<T> {
  try {
    val response = action.invoke()

    return ClientResult.Success(
      status = response.statusCode.toHttpStatus(),
      body = response.body,
    )
  } catch (e: WebClientResponseException) {
    return ClientResult.Failure.HttpResponse(
      e.request!!.method,
      e.request!!.uri.toString(),
      e.statusCode.toHttpStatus(),
      e.responseBodyAsString,
    )
  } catch (e: Exception) {
    return ClientResult.Failure.Other(e)
  }
}

private fun HttpStatusCode.toHttpStatus(): HttpStatus = HttpStatus.valueOf(this.value())
