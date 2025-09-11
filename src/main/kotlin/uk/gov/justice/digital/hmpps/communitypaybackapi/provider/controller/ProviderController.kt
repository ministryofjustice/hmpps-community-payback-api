package uk.gov.justice.digital.hmpps.communitypaybackapi.provider.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.controller.CommunityPaybackController
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.controller.getOrThrow
import uk.gov.justice.digital.hmpps.communitypaybackapi.provider.service.ProviderService

data class ProviderSummariesDto(
  @param:Schema(description = "List of Community Payback (UPW) providers")
  val providers: List<ProviderSummaryDto>,
)

data class ProviderSummaryDto(
  @param:Schema(description = "Community Payback (UPW) provider id", example = "1000")
  val id: Long,
  @param:Schema(description = "Community Payback (UPW) provider name", example = "East of England")
  val name: String,
)
data class ProviderTeamSummariesDto(
  @param:Schema(description = "List of Community Payback (UPW) provider teams for a given region")
  val providers: List<ProviderTeamSummaryDto>,
)

data class ProviderTeamSummaryDto(
  @param:Schema(description = "Community Payback (UPW) provider team id", example = "1001")
  val id: Long,
  @param:Schema(description = "Community Payback (UPW) provider team name", example = "Team Lincoln")
  val name: String,
)

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
      ApiResponse(responseCode = "401", description = "Unauthorized"),
      ApiResponse(responseCode = "403", description = "Forbidden"),
    ],
  )
  fun getProviders() = providerService.getProviders().getOrThrow()

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
      ApiResponse(responseCode = "401", description = "Unauthorized"),
      ApiResponse(responseCode = "403", description = "Forbidden"),
      ApiResponse(responseCode = "404", description = "Provider not found"),
    ],
  )
  fun getProviderTeam(@PathVariable providerId: Long): ProviderTeamSummariesDto = providerService.getProviderTeams(providerId).getOrThrow()
}
