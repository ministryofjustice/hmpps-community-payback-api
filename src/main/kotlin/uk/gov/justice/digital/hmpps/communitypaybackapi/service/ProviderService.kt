package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto

@Service
class ProviderService(
  val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
) {
  fun getProviders(username: String) = communityPaybackAndDeliusClient.getProviders(username).toDto()

  fun getProviderTeams(providerCode: String) = communityPaybackAndDeliusClient.getProviderTeams(providerCode).toDto()

  fun getTeamSupervisors(teamId: TeamId) = communityPaybackAndDeliusClient.getTeamSupervisors(teamId.providerCode, teamId.teamCode).toDto()

  fun getTeamUnallocatedSupervisor(teamId: TeamId) = getTeamSupervisors(teamId).supervisors.firstOrNull { it.unallocated } ?: error("Can't find unallocated supervisor for team '$teamId'")
}

data class TeamId(
  val providerCode: String,
  val teamCode: String,
)

fun ProjectDto.getTeamId() = TeamId(providerCode, teamCode)
