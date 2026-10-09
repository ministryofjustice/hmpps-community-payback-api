package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Duration
import java.time.LocalDate
import java.util.UUID

data class AdjustmentDto(
  val deliusId: Long,
  val id: UUID,
  val date: LocalDate,
  val amount: Duration,
  val reason: String,
  val reasonCode: String,
) {
  companion object
}

@Schema(description = "Filter adjustments by reason: TRAVEL_TIME includes TTX; OTHER excludes TTX")
enum class AdjustmentFilterTypeDto {
  TRAVEL_TIME,
  OTHER,
}
