package uk.gov.justice.digital.hmpps.communitypaybackapi.controller.supervisor

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentBulkUpdateService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@SupervisorUiController
@RequestMapping(
  "/supervisor/appointments",
  produces = [MediaType.APPLICATION_JSON_VALUE],
)
class SupervisorAppointmentsController(
  private val appointmentBulkUpdateService: AppointmentBulkUpdateService,
) {

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
  fun updateAppointmentOutcome(
    @RequestBody request: UpdateAppointmentOutcomesDto,
  ) = appointmentBulkUpdateService.updateAppointmentOutcomes(request)
}
