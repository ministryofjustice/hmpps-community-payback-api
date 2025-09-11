package uk.gov.justice.digital.hmpps.communitypaybackapi.project.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.CommunityPaybackController
import uk.gov.justice.digital.hmpps.communitypaybackapi.project.service.ProjectService
import java.time.LocalDate

data class ProjectAllocationsDto(
  @param:Schema(description = "List of project allocations")
  val allocations: List<ProjectAllocationDto>,
)

data class ProjectAllocationDto(
  @param:Schema(description = "Project allocation id", example = "1")
  val id: Long,
  @param:Schema(description = "Project name", example = "Community Garden Maintenance")
  val projectName: String,
  @param:Schema(description = "Team id", example = "1")
  val teamId: Long,
  @param:Schema(description = "Allocation start date", example = "2025-09-01")
  val startDate: LocalDate,
  @param:Schema(description = "Allocation end date", example = "2025-09-07")
  val endDate: LocalDate,
  @param:Schema(description = "Project code", example = "123")
  val projectCode: String,
  @param:Schema(description = "Number of offenders allocated", example = "12")
  val allocated: Int,
  @param:Schema(description = "Number of offenders with outcomes", example = "2")
  val outcomes: Int,
  @param:Schema(description = "Number of offenders with enforcements", example = "3")
  val enforcements: Int,
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
      ApiResponse(responseCode = "400", description = "Bad request - invalid date format or parameters"),
      ApiResponse(responseCode = "401", description = "Unauthorized"),
      ApiResponse(responseCode = "403", description = "Forbidden"),
      ApiResponse(responseCode = "404", description = "Team not found"),
    ],
  )
  fun getProjectAllocations(
    @Parameter(description = "Start date in format dd/MM/yyyy", example = "01/09/2025")
    @RequestParam
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
    @Parameter(description = "End date in format dd/MM/yyyy", example = "07/09/2025")
    @RequestParam
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
    @RequestParam teamId: Long,
  ): ProjectAllocationsDto = projectService.getProjectAllocations(startDate, endDate, teamId)
}
