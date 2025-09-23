package uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.UpdateAppointmentOutcomesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.service.AppointmentService
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.CommunityPaybackController
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@CommunityPaybackController
class AppointmentController(
  private val appointmentService: AppointmentService,
) {

  @PutMapping("/appointments")
  @Operation(
    description = """Record one or more appointment outcomes. This endpoint is idempotent. 
      If the most recent recorded outcome for a given delius appointment ID matches the values in the request, 
      nothing will be done for that delius appointment ID""",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      description = "Provides IDs of delius appointments to update, and the values to use for the update",
      content = [Content(mediaType = "application/json", schema = Schema(implementation = UpdateAppointmentOutcomesDto::class))],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Appointment update is (or has already) been recorded",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid appointment ID(s) provided",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun updateAppointments(@RequestBody updateAppointments: UpdateAppointmentOutcomesDto) {
    appointmentService.updateAppointmentsOutcome(updateAppointments)
  }
}
