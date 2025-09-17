package uk.gov.justice.digital.hmpps.communitypaybackapi.project.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.CommunityPaybackController
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.controller.OffenderDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.project.service.ProjectService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate
import java.time.LocalTime

data class ProjectAllocationsDto(
  @param:Schema(description = "List of project allocations")
  val allocations: List<ProjectAllocationDto>,
)

data class ProjectAllocationDto(
  @param:Schema(description = "Project allocation id", example = "1")
  val id: Long,
  val projectId: Long,
  @param:Schema(description = "Project name", example = "Community Garden Maintenance")
  val projectName: String,
  @param:Schema(description = "Project code", example = "123")
  val projectCode: String,
  @param:Schema(description = "Allocation date", example = "2025-09-01")
  val date: LocalDate,
  @param:Schema(description = "Allocation start time", example = "09:00")
  val startTime: LocalTime,
  @param:Schema(description = "Allocation end time", example = "17:00")
  val endTime: LocalTime,
  @param:Schema(description = "Number of offenders allocated", example = "12")
  val numberOfOffendersAllocated: Int,
  @param:Schema(description = "Number of offenders with outcomes", example = "2")
  val numberOfOffendersWithOutcomes: Int,
  @param:Schema(description = "Number of offenders with enforcements", example = "3")
  val numberOfOffendersWithEA: Int,
)

data class AppointmentsDto(
  val appointments: List<AppointmentDto>,
)

data class AppointmentDto(
  val id: Long,
  @param:Schema(description = "Project name", example = "Community Garden Maintenance")
  val projectName: String,
  @param:Schema(description = "How many community payback minutes the offender is required to complete", example = "2400")
  val requirementMinutes: Int,
  @param:Schema(description = "How many community payback minutes the offender has completed to date", example = "480")
  val completedMinutes: Int,
  val offender: OffenderDto,
)

@CommunityPaybackController
@RequestMapping("/projects")
class ProjectController(val projectService: ProjectService) {

  @GetMapping("/allocations")
  @Operation(
    description = "Get project allocations within date range for a specific team",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Successful project allocations response",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ProjectAllocationsDto::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request - invalid date format or parameters",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Team not found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getProjectAllocations(
    @Parameter(description = "Start date", example = "2025-09-01")
    @RequestParam
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
    @Parameter(description = "End date", example = "2025-09-01")
    @RequestParam
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
    @RequestParam teamId: Long,
  ): ProjectAllocationsDto = projectService.getProjectAllocations(startDate, endDate, teamId)

  @GetMapping("/{projectId}/appointments")
  @Operation(
    description = "Get project allocations within date range for a specific team",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Successful project allocations response",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = AppointmentsDto::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request - invalid date format or parameters",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getAppointments(
    @PathVariable projectId: Long,
    @Parameter(description = "Appointment date", example = "2025-01-01")
    @RequestParam
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
  ) = projectService.getAppointments(projectId, date)
}
