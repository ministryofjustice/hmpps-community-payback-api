package uk.gov.justice.digital.hmpps.communitypaybackapi.controller.internal

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeResultType
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentsOutcomesResultDto

fun UpdateAppointmentsOutcomesResultDto.toResponseEntity(): ResponseEntity<UpdateAppointmentsOutcomesResultDto> {
  val status = when {
    results.isNotEmpty() && results.all { it.result == UpdateAppointmentOutcomeResultType.SERVER_ERROR } -> HttpStatus.INTERNAL_SERVER_ERROR
    results.isNotEmpty() && results.all { it.result == UpdateAppointmentOutcomeResultType.VALIDATION_ERROR } -> HttpStatus.BAD_REQUEST
    else -> HttpStatus.OK
  }

  return ResponseEntity.status(status).body(this)
}
