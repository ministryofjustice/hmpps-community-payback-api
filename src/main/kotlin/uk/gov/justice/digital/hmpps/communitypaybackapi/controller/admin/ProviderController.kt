package uk.gov.justice.digital.hmpps.communitypaybackapi.controller.admin

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProviderSummariesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProviderTeamSummariesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SupervisorSummariesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.ProviderService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@AdminUiController
@RequestMapping(
  path = [ "/providers" ],
  produces = [MediaType.APPLICATION_JSON_VALUE],
)
class ProviderController(val providerService: ProviderService) {

  @GetMapping
  @Operation(
    description = "Get list of provider summaries",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Successful providers summaries response",
      ),
    ],
  )
  fun getProviders(): ProviderSummariesDto = providerService.getProviders()

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
}
