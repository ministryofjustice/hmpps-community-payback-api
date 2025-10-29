package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto

@Service
class ProviderService(
  val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
) {
  fun getProviders(username: String) = communityPaybackAndDeliusClient.getProviders(username).toDto()

  fun getProviderTeams(providerCode: String) = communityPaybackAndDeliusClient.getProviderTeams(providerCode).toDto()

  fun getTeamSupervisors(providerCode: String, teamCode: String) = communityPaybackAndDeliusClient.teamSupervisors(providerCode, teamCode).toDto()
}
