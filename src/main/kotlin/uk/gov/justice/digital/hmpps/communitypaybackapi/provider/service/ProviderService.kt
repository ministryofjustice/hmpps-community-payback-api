package uk.gov.justice.digital.hmpps.communitypaybackapi.provider.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CommunityPaybackAndDeliusClient

@Service
class ProviderService(
  val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
) {
  // we need to determine how to handle upstream errors
  fun getProviders() = communityPaybackAndDeliusClient.providers().toDto()

  @Suppress("UnusedParameter")
  fun getProviderTeams(providerId: Long) = communityPaybackAndDeliusClient.providerTeams().toDto()
}
