package uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.UpdateAppointmentOutcomesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.UpsertAppointmentDraftDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.service.AppointmentService
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.CommunityPaybackController
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@CommunityPaybackController
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

  @PutMapping(
    path = ["/appointments"],
    consumes = [MediaType.APPLICATION_JSON_VALUE],
  )
  @Operation(
    description = """Record one or more appointment outcomes. This endpoint is idempotent. 
      If the most recent recorded outcome for a given delius appointment ID matches the values in the request, 
      nothing will be done for that delius appointment ID""",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "Provides IDs of delius appointments to update, and the values to use for the update",
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

  @PatchMapping(
    path = ["/appointments/{deliusAppointmentId}/drafts"],
    consumes = [MediaType.APPLICATION_JSON_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @Operation(
    description = "Create or update an appointment draft for the given Delius appointment ID",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Draft created or updated",
      ),
    ],
  )
  fun upsertAppointmentDraft(
    @PathVariable deliusAppointmentId: Long,
    @RequestBody request: UpsertAppointmentDraftDto,
  ) = appointmentService.upsertAppointmentDraft(deliusAppointmentId, request)
}
