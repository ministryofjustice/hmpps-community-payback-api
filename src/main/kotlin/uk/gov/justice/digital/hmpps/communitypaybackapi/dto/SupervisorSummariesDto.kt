package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import io.swagger.v3.oas.annotations.media.Schema

data class SupervisorSummaryDto(
  @param:Schema(description = "Supervisor code", example = "P0123")
  val code: String,
  @param:Schema(description = "Supervisor name. Deprecated, use fullName", example = "John Smith", deprecated = true)
  val name: String,
  @param:Schema(description = "Supervisor name and grade", example = "John Smith [PO - Grade Description]")
  val fullName: String,
)

data class SupervisorSummariesDto(
  @param:Schema(description = "List of supervisors for a given team")
  val supervisors: List<SupervisorSummaryDto>,
)
