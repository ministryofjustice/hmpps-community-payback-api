package uk.gov.justice.digital.hmpps.communitypaybackapi.controller.supervisor

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.badRequest
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.notFound
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.DeliusAppointmentIdDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentEventTrigger
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.ContextService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.OffsetDateTime

@SupervisorUiController
@RequestMapping(
  "/supervisor/projects/{projectCode}/appointments",
  produces = [MediaType.APPLICATION_JSON_VALUE],
)
class SupervisorAppointmentsController(
  private val appointmentService: AppointmentService,
  private val contextService: ContextService,
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
  ): AppointmentDto {
    val id = DeliusAppointmentIdDto(projectCode, deliusAppointmentId)
    return appointmentService.getAppointment(id) ?: notFound("Appointment", id)
  }

  @PutMapping(
    path = ["/{deliusAppointmentId}"],
    consumes = [MediaType.APPLICATION_JSON_VALUE],
  )
  @Operation(
    description = "Record an appointment's outcome",
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
  fun updateAppointment(
    @PathVariable projectCode: String,
    @PathVariable deliusAppointmentId: Long,
    @RequestBody outcome: UpdateAppointmentDto,
  ) = updateAppointmentOutcome(projectCode, deliusAppointmentId, outcome.toUpdateAppointmentOutcomeDto())

  @PostMapping(
    path = ["/{deliusAppointmentId}/outcome"],
    consumes = [MediaType.APPLICATION_JSON_VALUE],
  )
  @Operation(
    deprecated = true,
    description = """Deprecated, instead use PUT /supervisor/projects/{projectCode}/appointments/{deliusAppointmentId}""",
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
    @RequestBody update: UpdateAppointmentOutcomeDto,
  ) {
    if (update.deliusId != deliusAppointmentId) {
      badRequest("ID in URL should match ID in payload")
    }

    val id = DeliusAppointmentIdDto(projectCode, deliusAppointmentId)
    val existingAppointment = appointmentService.getAppointment(id) ?: notFound("Appointment", id)

    appointmentService.updateAppointment(
      existingAppointment = existingAppointment,
      update = update,
      trigger = AppointmentEventTrigger(
        triggeredAt = OffsetDateTime.now(),
        triggerType = AppointmentEventTriggerType.USER,
        triggeredBy = contextService.getUserName(),
      ),
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
  fun updateAppointments(
    @PathVariable projectCode: String,
    @RequestBody request: UpdateAppointmentsDto,
  ) = appointmentService.updateAppointments(
    projectCode = projectCode,
    request = request.toUpdateAppointmentOutcomesDto(),
    trigger = AppointmentEventTrigger(
      triggeredAt = OffsetDateTime.now(),
      triggerType = AppointmentEventTriggerType.USER,
      triggeredBy = contextService.getUserName(),
    ),
  )
}
