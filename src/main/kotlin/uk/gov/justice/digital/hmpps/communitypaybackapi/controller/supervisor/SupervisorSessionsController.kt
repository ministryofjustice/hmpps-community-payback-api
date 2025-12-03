package uk.gov.justice.digital.hmpps.communitypaybackapi.controller.supervisor

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.SessionService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate

@SupervisorUiController
class SupervisorSessionsController(
  val sessionService: SessionService,
) {

  @GetMapping(
    path = [ "/supervisor/supervisors/{supervisorCode}/sessions/next"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @Operation(
    description = "Get the next session allocated to the supervisor or return 404 if there are no remaining future sessions. This includes sessions running today.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Successful response",
      ),
      ApiResponse(
        responseCode = "404",
        description = "There are no future sessions assigned to this supervisor code",
        content = [
          Content(
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getNextAllocation(
    @PathVariable supervisorCode: String,
  ) = sessionService.getNextAllocationForSupervisor(supervisorCode) ?: throw NotFoundException("There are no future sessions for supervisor $supervisorCode")

  @GetMapping(
    path = [ "/supervisor/providers/{providerCode}/teams/{teamCode}/sessions/future"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @Operation(
    description = "Get sessions allocated to the supervisor 7 days into the future. This includes sessions running today.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Successful response",
      ),
    ],
  )
  fun getFutureAllocations(
    @PathVariable providerCode: String,
    @PathVariable teamCode: String,
  ) = sessionService.getFutureAllocationsForSupervisor(providerCode, teamCode)

  @GetMapping(
    path = [ "/supervisor/projects/{projectCode}/sessions/{date}"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @Operation(
    description = "Get session information",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Successful session response",
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
