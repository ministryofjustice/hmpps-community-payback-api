package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

data class SupervisorTeamDto(
  val code: String,
  val description: String,
  val provider: ProviderSummaryDto,
)
