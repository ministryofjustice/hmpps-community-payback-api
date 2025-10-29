package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import io.swagger.v3.oas.annotations.media.Schema

data class SupervisorSummaryDto(
  @param:Schema(description = "Supervisor code", example = "P0123")
  val code: String,
  val name: NameDto,
  @param:Schema(
    description = "Supervisor name and grade. Deprecated, instead use individual elements in 'name' and 'grade'",
    example = "John Smith [PO - Grade Description]",
    deprecated = true,
  )
  val fullName: String,
  val grade: GradeDto?,
)

data class SupervisorSummariesDto(
  @param:Schema(description = "List of supervisors for a given team")
  val supervisors: List<SupervisorSummaryDto>,
)

data class GradeDto(
  val code: String,
  val description: String,
)
