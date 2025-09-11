package uk.gov.justice.digital.hmpps.communitypaybackapi.common.client

import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.GetExchange

interface CommunityPaybackAndDeliusClient {
  @GetExchange("/providers")
  fun providers(): ProviderSummaries

  @GetExchange("/provider-teams")
  fun providerTeams(@RequestParam providerId: Long): ProviderTeamSummaries
}

data class ProviderSummaries(
  val providers: List<ProviderSummary>,
)

data class ProviderSummary(
  val id: Long,
  val name: String,
)

data class ProviderTeamSummaries(
  val providers: List<ProviderTeamSummary>,
)

data class ProviderTeamSummary(
  val id: Long,
  val name: String,
)
