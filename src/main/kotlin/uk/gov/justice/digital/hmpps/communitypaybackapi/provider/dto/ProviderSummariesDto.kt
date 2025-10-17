package uk.gov.justice.digital.hmpps.communitypaybackapi.provider.dto

import io.swagger.v3.oas.annotations.media.Schema

data class ProviderSummaryDto(
  @param:Schema(description = "Community Payback (UPW) provider code", example = "ABC123")
  val code: String,
  @param:Schema(description = "Community Payback (UPW) provider name", example = "East of England")
  val name: String,
  @Deprecated("Id will be removed")
  @param:Schema(description = "Community Payback (UPW) provider id", example = "1000", deprecated = true)
  val id: Long = 0,
)

data class ProviderSummariesDto(
  @param:Schema(description = "List of Community Payback (UPW) providers")
  val providers: List<ProviderSummaryDto>,
)
