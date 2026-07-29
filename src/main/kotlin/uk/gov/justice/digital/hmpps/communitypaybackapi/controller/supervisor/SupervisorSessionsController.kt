package uk.gov.justice.digital.hmpps.communitypaybackapi.controller.supervisor

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springdoc.core.converters.models.PageableAsQueryParam
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import uk.gov.justice.digital.hmpps.communitypaybackapi.controller.internal.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectTypeGroupDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.SessionService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate

@SupervisorUiController
@RequestMapping(
  produces = [MediaType.APPLICATION_JSON_VALUE],
)
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

  @PageableAsQueryParam
  @GetMapping(
    path = ["/supervisor/sessions/recent"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @Operation(
    description = "Get recent group sessions that belong to the given teams.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Successful response",
      ),
    ],
  )
  fun getRecentSessions(
    @RequestParam teamCodes: List<String> = emptyList(),
    @RequestParam(defaultValue = "1") daysBefore: Long,
    @RequestParam(defaultValue = "0") daysAfter: Long,
    @Parameter(hidden = true)
    @PageableDefault(size = 50, sort = ["date"], direction = Sort.Direction.DESC) pageable: Pageable,
  ) = sessionService.getSessions(
    teamCodes,
    LocalDate.now().minusDays(daysBefore),
    LocalDate.now().plusDays(daysAfter),
    projectTypeGroup = ProjectTypeGroupDto.GROUP,
    pageable,
  )

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
