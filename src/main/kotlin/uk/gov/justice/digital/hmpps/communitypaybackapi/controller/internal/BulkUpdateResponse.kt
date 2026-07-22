package uk.gov.justice.digital.hmpps.communitypaybackapi.controller.internal

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeResultType
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentsOutcomesResultDto
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

fun UpdateAppointmentsOutcomesResultDto.toResponseEntity(): ResponseEntity<*> = when {
  results.isNotEmpty() && results.all { it.result == UpdateAppointmentOutcomeResultType.SERVER_ERROR } -> {
    val message = results.mapNotNull { it.errorMessage }.distinct().joinToString("; ")
      .ifEmpty { "Every appointment update failed with a server error" }
    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
      ErrorResponse(
        status = HttpStatus.INTERNAL_SERVER_ERROR,
        userMessage = "Unexpected error: $message",
        developerMessage = message,
      ),
    )
  }

  results.isNotEmpty() &&
    results.all { it.result == UpdateAppointmentOutcomeResultType.VALIDATION_ERROR } &&
    results.map { it.errorMessage }.distinct().size == 1 -> {
    val message = results.first().errorMessage ?: "Every appointment update failed validation"
    ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
      ErrorResponse(
        status = HttpStatus.BAD_REQUEST,
        userMessage = "Validation failure: $message",
        developerMessage = message,
      ),
    )
  }

  else -> ResponseEntity.ok(this)
}
