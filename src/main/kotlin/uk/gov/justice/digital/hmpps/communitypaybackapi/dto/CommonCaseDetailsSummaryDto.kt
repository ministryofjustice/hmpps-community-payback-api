package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class CommonCaseDetailsSummaryDto(
  val offender: OffenderDto,
  val unpaidWorkDetails: List<CommonUnpaidWorkDetailsDto> = emptyList(),
) {
  companion object
}

data class CommonUnpaidWorkDetailsDto(
  val eventNumber: Int,
  val sentenceDate: LocalDate,
  val requiredMinutes: Long,
  @param:Schema(description = "Minutes completed through attended appointments, before adjustments")
  val attendedAppointmentsMinutes: Long,
  @param:Schema(description = "Total adjustment minutes; negative values reduce the requirement")
  val totalAdjustmentMinutes: Long,
  @param:Schema(description = "Attended appointment minutes minus total adjustment minutes")
  val completedMinutes: Long,
  @param:Schema(description = "Required minutes minus completed minutes")
  val remainingMinutes: Long,
  @param:Schema(description = "The total number of minutes that can be credited to ETE appointments", example = "120")
  val allowedEteMinutes: Long,
  @param:Schema(description = "The total number of minutes credited to ETE appointments", example = "120")
  val completedEteMinutes: Long,
  @param:Schema(description = "The total number of remaining minutes that can be credited to ETE appointments", example = "80")
  val remainingEteMinutes: Long,
  val eventOutcome: String,
  val upwStatus: String?,
  val referralDate: LocalDate,
  val convictionDate: LocalDate?,
  val court: CourtDto?,
  val mainOffence: MainOffenceDto,
) {
  companion object
}
