package uk.gov.justice.digital.hmpps.communitypaybackapi.provider.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.CommunityPaybackController
import uk.gov.justice.digital.hmpps.communitypaybackapi.provider.dto.ProviderSummariesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.provider.dto.ProviderTeamSummariesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.provider.dto.SupervisorSummariesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.provider.service.ProviderService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@CommunityPaybackController
@RequestMapping("/providers")
class ProviderController(val providerService: ProviderService) {

  @GetMapping
  @Operation(
    description = "Get list of provider summaries",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Successful providers summaries response",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ProviderSummariesDto::class),
          ),
        ],
      ),
    ],
  )
  fun getProviders(): ProviderSummariesDto = providerService.getProviders()

  @GetMapping("/{providerId}/teams")
  @Operation(
    description = "Get team information for a specific provider",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Successful team response",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ProviderTeamSummariesDto::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Provider not found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getProviderTeam(@PathVariable providerId: Long): ProviderTeamSummariesDto = providerService.getProviderTeams(providerId)

  @GetMapping("/{providerId}/teams/{teamId}/supervisors")
  @Operation(
    description = "Get supervisor information for a specific team",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Successful supervisors response",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = SupervisorSummariesDto::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Provider or team not found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getTeamSupervisors(
    @PathVariable providerId: Long,
    @PathVariable teamId: Long,
  ): SupervisorSummariesDto = providerService.getTeamSupervisors(providerId, teamId)
}
