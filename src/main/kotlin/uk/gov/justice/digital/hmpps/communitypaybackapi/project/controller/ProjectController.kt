package uk.gov.justice.digital.hmpps.communitypaybackapi.project.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.CommunityPaybackController
import uk.gov.justice.digital.hmpps.communitypaybackapi.project.dto.ProjectAllocationsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.project.service.ProjectService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate
import java.time.LocalTime

@CommunityPaybackController
@RequestMapping("/projects")
class ProjectController(val projectService: ProjectService) {

  @GetMapping(
    path = [ "/allocations"],
    produces = [ APPLICATION_JSON_VALUE ],
  )
  @Operation(
    description = "Get project allocations within date range for a specific team",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Successful project allocations response",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request - invalid date format or parameters",
        content = [
          Content(
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Team not found",
        content = [
          Content(
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

  @GetMapping(
    path = [ "/{projectCode}/sessions/{date}/appointments"],
    produces = [ APPLICATION_JSON_VALUE ],
  )
  @Operation(
    description = "Get project allocations within date range for a specific team",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Successful project allocations response",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request - invalid date format or parameters",
        content = [
          Content(
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getSessions(
    @PathVariable projectCode: String,
    @Parameter(description = "Appointment date", example = "2025-01-01")
    @PathVariable
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
    @Parameter(description = "Appointment start time", example = "09:00")
    @RequestParam
    @DateTimeFormat(pattern = "HH:mm") start: LocalTime,
    @Parameter(description = "Appointment end time", example = "17:00")
    @RequestParam
    @DateTimeFormat(pattern = "HH:mm") end: LocalTime,
  ) = projectService.getSessions(projectCode, date, start, end)
}
