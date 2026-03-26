package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import jakarta.validation.constraints.Min
import java.time.LocalDate
import java.util.UUID

data class CreateAdjustmentDto(
  val taskId: UUID,
  val type: CreateAdjustmentTypeDto,
  @field:Min(value = 1)
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
