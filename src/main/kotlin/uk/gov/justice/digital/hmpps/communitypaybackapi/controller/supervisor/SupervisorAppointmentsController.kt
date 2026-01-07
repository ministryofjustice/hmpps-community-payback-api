package uk.gov.justice.digital.hmpps.communitypaybackapi.controller.supervisor

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
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentBulkUpdateService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentRetrievalService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentUpdateService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@SupervisorUiController
@RequestMapping(
  "/supervisor/projects/{projectCode}/appointments",
  produces = [MediaType.APPLICATION_JSON_VALUE],
)
class SupervisorAppointmentsController(
  private val appointmentRetrievalService: AppointmentRetrievalService,
  private val appointmentUpdateService: AppointmentUpdateService,
  private val appointmentBulkUpdateService: AppointmentBulkUpdateService,
) {

  @GetMapping(
    path = ["/{deliusAppointmentId}"],
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
  ) = appointmentRetrievalService.getAppointment(
    projectCode = projectCode,
    appointmentId = deliusAppointmentId,
  )

  @PostMapping(
    path = ["/{deliusAppointmentId}/outcome"],
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
  fun updateAppointmentOutcome(
    @PathVariable projectCode: String,
    @PathVariable deliusAppointmentId: Long,
    @RequestBody outcome: UpdateAppointmentOutcomeDto,
  ) {
    if (outcome.deliusId != deliusAppointmentId) {
      throw BadRequestException("ID in URL should match ID in payload")
    }

    appointmentUpdateService.updateAppointmentOutcome(
      update = outcome,
      projectCode = projectCode,
    )
  }

  @PostMapping(
    path = ["/bulk"],
    consumes = [MediaType.APPLICATION_JSON_VALUE],
  )
  @Operation(
    description = """Records one or more appointment outcomes. Note that if 200 is returned the response body must be checked to ensure all appointments have been updated""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Check the result JSON to check the outcome for each appointment update",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Validation error. If this occurs then no appointments have been updated",
        content = [
          Content(
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @SuppressWarnings("UnusedParameter")
  fun updateAppointmentOutcomes(
    @PathVariable projectCode: String,
    @RequestBody request: UpdateAppointmentOutcomesDto,
  ) = appointmentBulkUpdateService.updateAppointmentOutcomes(projectCode, request)
}
