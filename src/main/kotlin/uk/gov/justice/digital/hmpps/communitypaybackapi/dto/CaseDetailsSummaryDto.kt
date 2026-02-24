package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

data class CaseDetailsSummaryDto(
  val unpaidWorkDetails: List<CaseDetailDto> = emptyList(),
) {
  companion object
}

data class CaseDetailDto(
  val eventNumber: Long,
  val requiredMinutes: Long,
  val completedMinutes: Long,
  val adjustments: Long,
  val completedEteMinutes: Long,
) {
  companion object
}
