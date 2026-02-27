package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

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
  val completedEteMinutes: Long,
) {
  companion object
}
