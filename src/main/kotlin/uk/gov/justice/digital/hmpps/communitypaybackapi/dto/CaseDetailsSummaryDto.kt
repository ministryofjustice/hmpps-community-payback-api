package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class CaseDetailsSummaryDto(
  val unpaidWorkDetails: List<UnpaidWorkDetailsDto> = emptyList(),
) {
  companion object
}

data class UnpaidWorkDetailsDto(
  val eventNumber: Long,
  val sentenceDate: LocalDate,
  val requiredMinutes: Long,
  val completedMinutes: Long,
  val adjustments: Long,
  @param:Schema(description = "The total number of minutes that can be credited to ETE appointments", example = "120")
  val allowedEteMinutes: Long,
  @param:Schema(description = "The total number of minutes credited to ETE appointments", example = "120")
  val completedEteMinutes: Long,
  @param:Schema(description = "The total number of remaining minutes that can be credited to ETE appointments", example = "80")
  val remainingEteMinutes: Long,
) {
  companion object
}
