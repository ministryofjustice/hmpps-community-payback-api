package uk.gov.justice.digital.hmpps.communitypaybackapi.provider.service

import org.springframework.stereotype.Service

@Suppress("MagicNumber")
val PROVIDER_SUMMARIES = ProviderSummaries(
  listOf(
    ProviderSummary(1000, "East of England"),
    ProviderSummary(2000, "North East Region"),
    ProviderSummary(3000, "North West Region"),
  ),
)

@Suppress("MagicNumber")
val PROVIDER_TEAMS = ProviderTeamSummaries(
  listOf(
    ProviderTeamSummary(1001, "Team Lincoln"),
    ProviderTeamSummary(2001, "Team Grantham"),
    ProviderTeamSummary(3001, "Team Boston"),
  ),
)

@Service
class ProviderService {
  fun getProviders() = PROVIDER_SUMMARIES.toDto()

  @Suppress("UnusedParameter")
  fun getProviderTeams(providerId: Long) = PROVIDER_TEAMS.toDto()
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
