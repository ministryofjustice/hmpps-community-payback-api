package uk.gov.justice.digital.hmpps.communitypaybackapi.controller.admin

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springdoc.core.converters.models.PageableAsQueryParam
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import uk.gov.justice.digital.hmpps.communitypaybackapi.controller.internal.notFound
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentTaskSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentTaskService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate
import java.util.UUID

@AdminUiController
@RequestMapping(
  "/admin",
  produces = [MediaType.APPLICATION_JSON_VALUE],
)
class AdminAppointmentTaskController(
  private val appointmentTaskService: AppointmentTaskService,
) {

  @GetMapping(
    path = ["/appointment-tasks/pending"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @Operation(
    description = "Get pending appointment tasks with optional filters",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Successful retrieval of pending appointment tasks",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad Request",
        content = [
          Content(
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @PageableAsQueryParam
  fun getPendingAppointmentTasks(
    @Parameter(
      hidden = true,
      description = "Pagination parameters. Default sort: createdAt DESC, size: 50",
      schema = Schema(
        implementation = Pageable::class,
        description = "Pagination parameters for appointment tasks",
      ),
    )
    @PageableDefault(size = 50, sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) appointmentFromDate: LocalDate?,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) appointmentToDate: LocalDate?,
    @RequestParam(required = false) appointmentProviderCode: String?,
  ): Page<AppointmentTaskSummaryDto> = appointmentTaskService.getPendingAppointmentTasks(
    fromDate = appointmentFromDate,
    toDate = appointmentToDate,
    providerCode = appointmentProviderCode,
    pageable = pageable,
  )

  @PutMapping("/appointment-tasks/{taskId}/complete")
  @Operation(
    description = "Marks a task as complete",
    responses = [
      ApiResponse(responseCode = "204", description = "Processed"),
      ApiResponse(responseCode = "404", description = "Task not found", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    ],
  )
  fun completeTask(@PathVariable taskId: UUID) {
    if (!appointmentTaskService.taskExists(taskId)) {
      notFound("Task", taskId)
    }
    appointmentTaskService.completeTask(taskId)
  }
}
