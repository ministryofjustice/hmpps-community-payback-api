package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

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
