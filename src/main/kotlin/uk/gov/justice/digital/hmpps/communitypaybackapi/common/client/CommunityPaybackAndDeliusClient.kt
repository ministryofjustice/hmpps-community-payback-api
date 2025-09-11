package uk.gov.justice.digital.hmpps.communitypaybackapi.common.client

import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.GetExchange
import java.time.LocalDate

interface CommunityPaybackAndDeliusClient {
  @GetExchange("/providers")
  fun providers(): ProviderSummaries

  @GetExchange("/provider-teams")
  fun providerTeams(): ProviderTeamSummaries

  @GetExchange("/project-allocations")
  fun getProjectAllocations(
    @RequestParam startDate: LocalDate,
    @RequestParam endDate: LocalDate,
    @RequestParam teamId: Long,
  ): ProjectAllocations
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
data class ProjectAllocations(
  val allocations: List<ProjectAllocation>,
)

data class ProjectAllocation(
  val id: Long,
  val projectName: String,
  val teamId: Long,
  val startDate: LocalDate,
  val endDate: LocalDate,
  val hours: Int,
)
