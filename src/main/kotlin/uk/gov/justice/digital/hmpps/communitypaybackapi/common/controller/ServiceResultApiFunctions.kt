package uk.gov.justice.digital.hmpps.communitypaybackapi.common.controller

import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.ServiceResult
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

fun <T> ServiceResult<T>.getOrThrow(): T = when (this) {
  is ServiceResult.Success<T> -> value
  is ServiceResult.Error.BadRequest -> throw HmppsErrorResponseException(
    ErrorResponse(
      status = HttpStatus.BAD_REQUEST,
      userMessage = message,
      developerMessage = message,
    ),
  )
  is ServiceResult.Error.NotFound<T> -> throw HmppsErrorResponseException(
    ErrorResponse(
      status = HttpStatus.NOT_FOUND,
      userMessage = "Could not find $entityType for id '$id'",
      developerMessage = "Could not find $entityType for id '$id'",
    ),
  )
}

class HmppsErrorResponseException(val response: ErrorResponse) : Exception(response.developerMessage)
