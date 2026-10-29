package uk.gov.justice.digital.hmpps.communitypaybackapi.controller.common

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
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectTypeGroupDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.SessionService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate

@CommonController
@RequestMapping(
  "/common/teams",
  produces = [MediaType.APPLICATION_JSON_VALUE],
)
class CommonTeamController(
  private val sessionService: SessionService,
) {
  @PageableAsQueryParam
  @GetMapping("{teamCode}/sessions")
  @Operation(
    description = "Get sessions within a date range for a specific team",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Successful sessions response",
      ),
      ApiResponse(
        responseCode = "404",
        description = "Provider or team not found",
        content = [
          Content(
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getSessions(
    @PathVariable teamCode: String,
    @RequestParam
    @Parameter(description = "Start date, inclusive", example = "2025-09-01")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
    @RequestParam
    @Parameter(description = "End date, inclusive", example = "2025-09-01")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
    @RequestParam("projectType", required = false)
    projectTypeCodes: List<String>?,
    @Parameter(hidden = true)
    @PageableDefault(size = 50, sort = ["projectName"], direction = Sort.Direction.ASC) pageable: Pageable,
  ) = when (projectTypeCodes) {
    null -> sessionService.getSessions(
      teamCodes = listOf(teamCode),
      startDate = startDate,
      endDate = endDate,
      projectTypeGroup = ProjectTypeGroupDto.GROUP,
      pageable = pageable,
    )
    else -> sessionService.getSessions(
      teamCodes = listOf(teamCode),
      startDate = startDate,
      endDate = endDate,
      projectTypeCodes = projectTypeCodes,
      pageable = pageable,
    )
  }
}
