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
import org.springframework.web.bind.annotation.RequestMapping
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.BadRequestException
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@AdminUiController
@RequestMapping(
  "/admin",
  produces = [MediaType.APPLICATION_JSON_VALUE],
)
class AdminAppointmentController(
  private val appointmentService: AppointmentService,
) {

  @GetMapping(
    path = ["/appointments/{deliusAppointmentId}"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @Operation(
    description = "Get appointment given its ID. Deprecated, use '/projects/{projectCode}/appointments/{deliusAppointmentId}'",
    deprecated = true,
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
  @Deprecated("Use version that takes projectCode")
  fun getAppointment(
    @PathVariable deliusAppointmentId: Long,
  ) = appointmentService.getAppointment(
    projectCode = "UNKNOWN",
    appointmentId = deliusAppointmentId,
  )

  @GetMapping(
    path = ["/projects/{projectCode}/appointments/{deliusAppointmentId}"],
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
    @PathVariable projectCode: String,
    @PathVariable deliusAppointmentId: Long,
  ) = appointmentService.getAppointment(
    projectCode = projectCode,
    appointmentId = deliusAppointmentId,
  )

  @PostMapping(
    path = ["/appointments/{deliusAppointmentId}/outcome"],
    consumes = [MediaType.APPLICATION_JSON_VALUE],
  )
  @Operation(
    description = """Record an appointment's outcome. This endpoint is idempotent -  
      If the most recent recorded outcome matches the values in the request nothing will be done and a 200 will be returned.
      Deprecated, use POST '/projects/{projectCode}/appointments/{deliusAppointmentId}/outcome'""",
    deprecated = true,
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
      ApiResponse(
        responseCode = "409",
        description = "A newer version of the appointment exists in Delius",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @Deprecated("Use version that takes projectCode")
  fun updateAppointmentOutcomeDeprecated(
    @PathVariable deliusAppointmentId: Long,
    @RequestBody outcome: UpdateAppointmentOutcomeDto,
  ) {
    if (outcome.deliusId != deliusAppointmentId) {
      throw BadRequestException("ID in URL should match ID in payload")
    }

    appointmentService.updateAppointmentOutcome(
      projectCode = "UNKNOWN",
      outcome = outcome,
    )
  }

  @PostMapping(
    path = ["/projects/{projectCode}/appointments/{deliusAppointmentId}/outcome"],
    consumes = [MediaType.APPLICATION_JSON_VALUE],
  )
  @Operation(
    description = """Record an appointment's outcome. This endpoint is idempotent -  
      If the most recent recorded outcome matches the values in the request nothing will be done and a 200 will be returned.""",
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
      ApiResponse(
        responseCode = "409",
        description = "A newer version of the appointment exists in Delius",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @SuppressWarnings("UnusedParameter")
  fun updateAppointmentOutcome(
    @PathVariable projectCode: String,
    @PathVariable deliusAppointmentId: Long,
    @RequestBody outcome: UpdateAppointmentOutcomeDto,
  ) {
    if (outcome.deliusId != deliusAppointmentId) {
      throw BadRequestException("ID in URL should match ID in payload")
    }

    appointmentService.updateAppointmentOutcome(
      projectCode = projectCode,
      outcome = outcome,
    )
  }
}
