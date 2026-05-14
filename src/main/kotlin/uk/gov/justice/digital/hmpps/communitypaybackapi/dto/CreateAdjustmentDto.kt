package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import java.time.LocalDate
import java.util.UUID

data class CreateAdjustmentDto(
  val taskId: UUID,
  @param:Schema(description = "Positive will increase minutes required. Negative will reduce minutes required.")
  val type: CreateAdjustmentTypeDto,
  @field:Min(value = 1)
  @param:Schema(description = "Adjustment minutes, must be greater than 0")
  val minutes: Int,
  val dateOfAdjustment: LocalDate,
  val adjustmentReasonId: UUID,
) {
  companion object
}

enum class CreateAdjustmentTypeDto {
  Positive,
  Negative,
}
