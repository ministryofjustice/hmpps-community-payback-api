package uk.gov.justice.digital.hmpps.communitypaybackapi.controller.admin

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springdoc.core.converters.models.PageableAsQueryParam
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.data.web.PagedModel
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentSummariesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectTypeGroupDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.BadRequestException
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentEventTrigger
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentRetrievalService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentUpdateService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.ContextService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate
import java.time.OffsetDateTime

@AdminUiController
@RequestMapping(
  "/admin",
  produces = [MediaType.APPLICATION_JSON_VALUE],
)
class AdminAppointmentController(
  private val appointmentRetrievalService: AppointmentRetrievalService,
  private val appointmentUpdateService: AppointmentUpdateService,
  private val contextService: ContextService,
) {

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
  ) = appointmentRetrievalService.getAppointment(
    projectCode = projectCode,
    appointmentId = deliusAppointmentId,
  )

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
  @Suppress("UnusedParameter")
  fun updateAppointmentOutcome(
    @PathVariable projectCode: String,
    @PathVariable deliusAppointmentId: Long,
    @RequestBody outcome: UpdateAppointmentOutcomeDto,
  ) {
    if (outcome.deliusId != deliusAppointmentId) {
      throw BadRequestException("ID in URL should match ID in payload")
    }

    appointmentUpdateService.updateAppointmentOutcome(
      projectCode = projectCode,
      update = outcome,
      trigger = AppointmentEventTrigger(
        triggeredAt = OffsetDateTime.now(),
        triggerType = AppointmentEventTriggerType.USER,
        triggeredBy = contextService.getUserName(),
      ),
    )
  }

  @GetMapping(
    path = ["/appointments"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @Operation(
    description = "Get filtered appointments. At least one filter parameter must be provided.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Successful appointment response",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad Request - No filter parameters provided",
        content = [
          Content(
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @Suppress("UnusedParameter")
  @PageableAsQueryParam
  fun getAppointments(
    @Parameter(
      description = "Pagination and sorting parameters. Supported sort fields: crn, name, date, startTime, endTime, daysOverdue. Default sort: crn DESC, size: 50",
      schema = Schema(
        implementation = Pageable::class,
        description = "Only crn, name, date, startTime, endTime and daysOverdue fields are supported for sorting",
      ),
    )
    @PageableDefault(size = 50, sort = ["crn"], direction = Sort.Direction.DESC) pageable: Pageable,
    @RequestParam(required = false) crn: String?,
    @Parameter(
      description = "Filter by one or more project codes",
      array = ArraySchema(schema = Schema(type = "string")),
      example = "[\"N56Vfhsef\",\"N56LBMimO\"]",
    )
    @RequestParam(required = false) projectCodes: List<String>?,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) fromDate: LocalDate?,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) toDate: LocalDate?,
    @Parameter(
      description = "Filter by one or more outcome codes",
      array = ArraySchema(schema = Schema(type = "string")),
      example = "[\"ATTC\",\"NO_OUTCOME\"]",
    )
    @RequestParam(required = false) outcomeCodes: List<String>?,
    @RequestParam projectTypeGroup: ProjectTypeGroupDto?,
  ): PagedModel<AppointmentSummariesDto> {
    val hasFilter = !crn.isNullOrBlank() ||
      !projectCodes.isNullOrEmpty() ||
      fromDate != null ||
      toDate != null ||
      !outcomeCodes.isNullOrEmpty() ||
      projectTypeGroup != null

    if (!hasFilter) {
      throw BadRequestException("At least one filter parameter must be provided")
    }

    throw ResponseStatusException(
      org.springframework.http.HttpStatus.NOT_IMPLEMENTED,
      "Endpoint not yet implemented",
    )
  }
}
