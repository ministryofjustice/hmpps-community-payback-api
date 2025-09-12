package uk.gov.justice.digital.hmpps.communitypaybackapi.common.client

import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.GetExchange
import java.time.LocalDate
import java.time.LocalTime

interface CommunityPaybackAndDeliusClient {
  @GetExchange("/providers")
  fun providers(): ProviderSummaries

  @GetExchange("/provider-teams")
  fun providerTeams(@RequestParam providerId: Long): ProviderTeamSummaries

  @GetExchange("/project-allocations")
  fun getProjectAllocations(
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
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
  val teams: List<ProviderTeamSummary>,
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
  val date: LocalDate,
  val projectName: String,
  val projectCode: String,
  val startTime: LocalTime,
  val endTime: LocalTime,
  val numberOfOffendersAllocated: Int,
  val numberOfOffendersWithOutcomes: Int,
  val numberOfOffendersWithEA: Int,
)
