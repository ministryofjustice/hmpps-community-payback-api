package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.PickUpLocationsDto
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

  fun getPickupLocations(teamId: TeamId) = try {
    PickUpLocationsDto(
      communityPaybackAndDeliusClient.getTeamLocations(teamId.teamCode).locations.map { it.toDto() },
    )
  } catch (_: WebClientResponseException.NotFound) {
    null
  }

  fun getPickupLocation(teamId: TeamId, pickupLocationCode: String) = getPickupLocations(teamId)?.locations?.firstOrNull {
    it.deliusCode == pickupLocationCode
  }
}

data class TeamId(
  val providerCode: String,
  val teamCode: String,
)

fun ProjectDto.getTeamId() = TeamId(providerCode, teamCode)
