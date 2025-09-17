package uk.gov.justice.digital.hmpps.communitypaybackapi.provider.dto

import io.swagger.v3.oas.annotations.media.Schema

data class SupervisorSummaryDto(
  @param:Schema(description = "Supervisor id", example = "4")
  val id: Long,
  @param:Schema(description = "Supervisor name", example = "John Smith")
  val name: String,
)

data class SupervisorSummariesDto(
  @param:Schema(description = "List of supervisors for a given team")
  val supervisors: List<SupervisorSummaryDto>,
)
