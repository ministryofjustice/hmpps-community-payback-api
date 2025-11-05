package uk.gov.justice.digital.hmpps.communitypaybackapi.controller.admin

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SessionSummariesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.SessionService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate

@AdminUiController
@RequestMapping(
  "/admin/projects",
  produces = [MediaType.APPLICATION_JSON_VALUE],
)
class SessionController(val sessionService: SessionService) {

  @GetMapping(
    path = [ "/session-search"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @Operation(
    description = "Get project sessions within date range for a specific team",
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
  fun getSessions(
    @Parameter(description = "Start date", example = "2025-09-01")
    @RequestParam
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
    @Parameter(description = "End date", example = "2025-09-01")
    @RequestParam
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
    @Parameter(description = "Team Code", example = "ABC123")
    @RequestParam teamCode: String,
  ): SessionSummariesDto = sessionService.getSessions(startDate, endDate, teamCode)

  @GetMapping(
    path = [ "/{projectCode}/sessions/{date}"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
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
  fun getSession(
    @PathVariable projectCode: String,
    @Parameter(description = "Date", example = "2025-01-01")
    @PathVariable
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
  ) = sessionService.getSession(projectCode, date)
}
