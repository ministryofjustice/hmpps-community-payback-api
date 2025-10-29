package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import io.swagger.v3.oas.annotations.media.Schema

data class ProviderSummaryDto(
  @param:Schema(description = "Community Payback (UPW) provider code", example = "ABC123")
  val code: String,
  @param:Schema(description = "Community Payback (UPW) provider name", example = "East of England")
  val name: String,
)

data class ProviderSummariesDto(
  @param:Schema(description = "List of Community Payback (UPW) providers")
  val providers: List<ProviderSummaryDto>,
)
