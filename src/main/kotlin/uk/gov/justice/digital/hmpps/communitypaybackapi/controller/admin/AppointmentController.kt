package uk.gov.justice.digital.hmpps.communitypaybackapi.controller.admin

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@AdminUiController
class AppointmentController(
  private val appointmentService: AppointmentService,
) {

  @GetMapping(
    path = ["/appointments/{appointmentId}"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @Operation(
    description = "Get appointment given its ID",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Successful appointment response",
      ),
      ApiResponse(
        responseCode = "404",
        description = "Invalid appointment ID",
        content = [
          Content(
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getAppointment(
    @PathVariable appointmentId: Long,
  ) = appointmentService.getAppointment(appointmentId)

  @PostMapping(
    path = ["/appointments/{deliusAppointmentId}/outcome"],
    consumes = [MediaType.APPLICATION_JSON_VALUE],
  )
  @Operation(
    description = """Record an appointment's outcome. This endpoint is idempotent -  
      If the most recent recorded outcome matches the values in the request nothing will be done and a 200 will be returned""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Appointment update is (or has already) been recorded",
      ),
      ApiResponse(
        responseCode = "404",
        description = "Invalid appointment ID provided",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun updateAppointmentOutcome(
    @PathVariable deliusAppointmentId: Long,
    @RequestBody outcome: UpdateAppointmentOutcomeDto,
  ) {
    appointmentService.updateAppointmentOutcome(
      deliusId = deliusAppointmentId,
      outcome = outcome,
    )
  }
}
