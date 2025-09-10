package uk.gov.justice.digital.hmpps.communitypaybackapi.common.client

import org.springframework.web.service.annotation.GetExchange

interface CommunityPaybackAndDeliusClient {
  @GetExchange("/providers")
  fun providers(): ProviderSummaries
}

data class ProviderSummaries(
  val providers: List<ProviderSummary>,
)

data class ProviderSummary(
  val id: Long,
  val name: String,
)
