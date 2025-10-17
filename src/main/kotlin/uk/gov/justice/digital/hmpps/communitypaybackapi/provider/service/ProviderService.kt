package uk.gov.justice.digital.hmpps.communitypaybackapi.provider.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CommunityPaybackAndDeliusClient

@Service
class ProviderService(
  val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
) {
  fun getProviders() = communityPaybackAndDeliusClient.getProviders().toDto()

  fun getProviderTeams(providerCode: String) = communityPaybackAndDeliusClient.getProviderTeams(providerCode).toDto()

  fun getTeamSupervisors(providerCode: String, teamCode: String) = communityPaybackAndDeliusClient.teamSupervisors(providerCode, teamCode).toDto()
}
