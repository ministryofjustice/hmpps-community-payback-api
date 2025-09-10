package uk.gov.justice.digital.hmpps.communitypaybackapi.provider.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.CommunityPaybackController
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

@CommunityPaybackController
@RequestMapping("/providers")
class ProviderController(val providerService: ProviderService) {

  @GetMapping(produces = ["application/json"])
  @Operation(
    summary = "Get list of provider summaries API",
    description = "Get list of provider summaries API",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Successful providers summaries response",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ProviderSummaryDto::class),
          ),
        ],
      ),
      ApiResponse(responseCode = "401", description = "Unauthorized"),
      ApiResponse(responseCode = "403", description = "Forbidden"),
    ],
  )
  fun getProviders(): ProviderSummariesDto = providerService.getProviders()
}
