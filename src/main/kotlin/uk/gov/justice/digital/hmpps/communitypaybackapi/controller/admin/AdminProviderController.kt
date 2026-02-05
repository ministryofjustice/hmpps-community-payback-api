package uk.gov.justice.digital.hmpps.communitypaybackapi.controller.admin

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus.NOT_IMPLEMENTED
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectSummariesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectTypeGroupDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProviderSummariesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProviderTeamSummariesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SupervisorSummariesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.ProviderService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.SessionService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate

@AdminUiController
@RequestMapping(
  "/admin/providers",
  produces = [MediaType.APPLICATION_JSON_VALUE],
)
class AdminProviderController(
  val providerService: ProviderService,
  val sessionService: SessionService,
) {

  @GetMapping
  @Operation(
    description = "Get list of provider summaries available for a given user",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Successful providers summaries response",
      ),
    ],
  )
  fun getProviders(
    @RequestParam username: String,
  ): ProviderSummariesDto = providerService.getProviders(username)

  @GetMapping("/{providerCode}/teams")
  @Operation(
    description = "Get team information for a specific provider",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Successful team response",
      ),
      ApiResponse(
        responseCode = "404",
        description = "Provider not found",
        content = [
          Content(
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getProviderTeam(@PathVariable providerCode: String): ProviderTeamSummariesDto = providerService.getProviderTeams(providerCode)

  @GetMapping("/{providerCode}/teams/{teamCode}/supervisors")
  @Operation(
    description = "Get supervisor information for a specific team",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Successful supervisors response",
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
  fun getTeamSupervisors(
    @PathVariable providerCode: String,
    @PathVariable teamCode: String,
  ): SupervisorSummariesDto = providerService.getTeamSupervisors(providerCode, teamCode)

  @GetMapping("/{providerCode}/teams/{teamCode}/sessions")
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
    @PathVariable providerCode: String,
    @PathVariable teamCode: String,
    @RequestParam
    @Parameter(description = "Start date, inclusive", example = "2025-09-01")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
    @RequestParam
    @Parameter(description = "End date, inclusive", example = "2025-09-01")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
  ) = sessionService.getSessions(
    providerCode,
    teamCode,
    startDate,
    endDate,
    ProjectTypeGroupDto.GROUP,
  )

  @GetMapping("/{providerCode}/teams/{teamCode}/projects")
  @Operation(
    description = "Get projects for a specific team",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Successful projects response",
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
  @Suppress("UnusedParameter")
  fun getProjects(
    @Parameter(
      description = "Pagination and sorting parameters. Supported sort fields: projectName Default sort: projectName DESC, size: 50",
      schema = Schema(
        implementation = Pageable::class,
        description = "Only projectName. numberOfAppointmentsOverdue and oldestOverdueAppointmentInDays fields are supported for sorting",
      ),
    )
    @PageableDefault(size = 50, sort = ["projectName"], direction = Sort.Direction.DESC) pageable: Pageable,
    @PathVariable providerCode: String,
    @PathVariable teamCode: String,
    @RequestParam projectTypeGroup: ProjectTypeGroupDto,
  ): ProjectSummariesDto = throw ResponseStatusException(NOT_IMPLEMENTED, "Not Implemented")
}
