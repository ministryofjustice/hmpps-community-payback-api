package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import java.util.UUID

data class AdjustmentReasonDto(
  val id: UUID,
  val name: String,
  val hintText: String?,
  val deliusCode: String,
  val maxMinutesAllowed: Int,
  val rank: Int,
) {
  companion object
}

data class AdjustmentReasonsDto(
  val adjustmentReasons: List<AdjustmentReasonDto>,
)
