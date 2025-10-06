package uk.gov.justice.digital.hmpps.communitypaybackapi.provider.dto

import io.swagger.v3.oas.annotations.media.Schema

data class ProviderTeamSummaryDto(
  @param:Schema(description = "Community Payback (UPW) provider team id", example = "1001")
  val id: Long,
  @param:Schema(description = "Community Payback (UPW) provider team code", example = "ABD123")
  val code: String,
  @param:Schema(description = "Community Payback (UPW) provider team name", example = "Team Lincoln")
  val name: String,
)

data class ProviderTeamSummariesDto(
  @param:Schema(description = "List of Community Payback (UPW) provider teams for a given region")
  val providers: List<ProviderTeamSummaryDto>,
)
