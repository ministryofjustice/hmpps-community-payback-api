package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalTime

data class SessionSummariesDto(
  @param:Schema(description = "List of project allocations")
  val allocations: List<SessionSummaryDto>,
)

data class SessionSummaryDto(
  @Deprecated("Id will be removed")
  @param:Schema(description = "Project allocation id", example = "1", deprecated = true)
  val id: Long? = null,
  @Deprecated("Project id will be removed")
  @param:Schema(deprecated = true)
  val projectId: Long? = null,
  @param:Schema(description = "Project name", example = "Community Garden Maintenance")
  val projectName: String,
  @param:Schema(description = "Project code", example = "123")
  val projectCode: String,
  @param:Schema(description = "Allocation date", example = "2025-09-01")
  val date: LocalDate,
  @param:Schema(description = "Allocation start local time", example = "09:00", pattern = "^([0-1][0-9]|2[0-3]):[0-5][0-9]$")
  val startTime: LocalTime,
  @param:Schema(description = "Allocation end local time", example = "17:00", pattern = "^([0-1][0-9]|2[0-3]):[0-5][0-9]$")
  val endTime: LocalTime,
  @param:Schema(description = "Number of offenders allocated", example = "12")
  val numberOfOffendersAllocated: Int,
  @param:Schema(description = "Number of offenders with outcomes", example = "2")
  val numberOfOffendersWithOutcomes: Int,
  @param:Schema(description = "Number of offenders with enforcements", example = "3")
  val numberOfOffendersWithEA: Int,
)
