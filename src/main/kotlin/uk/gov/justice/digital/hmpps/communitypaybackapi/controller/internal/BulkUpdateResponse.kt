package uk.gov.justice.digital.hmpps.communitypaybackapi.controller.internal

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.ApiResponse.ErrorResponse
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.ApiResponseIssue
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeResultType
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentsOutcomesResultDto

fun UpdateAppointmentsOutcomesResultDto.toResponseEntity(): ResponseEntity<*> = when {
  results.isNotEmpty() && results.all { it.result == UpdateAppointmentOutcomeResultType.SERVER_ERROR } -> {
    val message = results.mapNotNull { it.errorMessage }.distinct().joinToString("; ")
      .ifEmpty { "Every appointment update failed with a server error" }
    val errors = results.mapIndexed { index, result -> ApiResponseIssue("$.updates[$index]", "UNKNOWN_ERROR", mapOf("message" to result.errorMessage)) }
    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
      ErrorResponse<Unit>(500, errors, emptyList(), null, message),
    )
  }

  results.isNotEmpty() &&
    results.all { it.result == UpdateAppointmentOutcomeResultType.VALIDATION_ERROR } &&
    results.map { it.errorMessage }.distinct().size == 1 -> {
    val message = results.first().errorMessage ?: "Every appointment update failed validation"
    val error = ApiResponseIssue("", "GENERIC_VALIDATION_ERROR", mapOf("message" to message))
    ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
      ErrorResponse<Unit>(400, listOf(error), emptyList(), "Validation failure: $message", message),
    )
  }

  else -> ResponseEntity.ok(this)
}
