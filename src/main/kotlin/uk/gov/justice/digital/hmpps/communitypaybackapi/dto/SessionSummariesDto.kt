package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class SessionSummariesDto(
  @param:Schema(
    deprecated = true,
    description = """
      Deprecated: use the `content` property instead.
      
      List of project allocations
    """,
  )
  val allocations: List<SessionSummaryDto>,
  val content: List<SessionSummaryDto>,
  val page: PageMetaDto,
)

data class SessionSummaryDto(
  @param:Schema(description = "Project name", example = "Community Garden Maintenance")
  val projectName: String,
  @param:Schema(description = "Project code", example = "123")
  val projectCode: String,
  @param:Schema(description = "Allocation date", example = "2025-09-01")
  val date: LocalDate,
  @param:Schema(description = "Number of offenders allocated", example = "12")
  val numberOfOffendersAllocated: Int,
  @param:Schema(description = "Number of offenders with outcomes", example = "2")
  val numberOfOffendersWithOutcomes: Int,
  @param:Schema(description = "Number of offenders with outcomes requiring enforcement", example = "3")
  val numberOfOffendersWithEA: Int,
)
