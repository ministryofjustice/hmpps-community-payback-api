package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class CaseDetailsSummaryDto(
  val offender: OffenderDto,
  val unpaidWorkDetails: List<UnpaidWorkDetailsDto> = emptyList(),
) {
  companion object
}

data class UnpaidWorkDetailsDto(
  val eventNumber: Int,
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
  val eventOutcome: String,
  val upwStatus: String?,
  val referralDate: LocalDate,
  val convictionDate: LocalDate,
  val court: CourtDto,
  val mainOffence: MainOffenceDto,
) {
  companion object
}

data class MainOffenceDto(
  val date: LocalDate,
  val count: Int,
  val code: String,
  val description: String,
) {
  companion object
}

data class CourtDto(
  val code: String,
  val description: String,
) {
  companion object
}
