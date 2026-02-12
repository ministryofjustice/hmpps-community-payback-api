package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalTime

data class AppointmentSummaryDto(
  val id: Long,
  @param:Schema(
    description = "How many community payback minutes the offender is required to complete",
    example = "2400",
  )
  val contactOutcome: ContactOutcomeDto?,
  @param:Schema(description = "Total minutes ordered. >= 0", example = "480")
  val requirementMinutes: Int,
  @param:Schema(description = "Adjustment minutes. Can positive or negative e.g. +50 means an additional 50 minutes have been added to the requirement", example = "-60")
  val adjustmentMinutes: Int,
  @param:Schema(description = "How many community payback minutes the offender has completed to date. >= 0", example = "280")
  val completedMinutes: Int,
  val offender: OffenderDto,
  val date: LocalDate?, // Remove this nullability when session search changes to new endpoints
  val startTime: LocalTime?, // Remove this nullability when session search changes to new endpoints
  val endTime: LocalTime?, // Remove this nullability when session search changes to new endpoints
  val daysOverdue: Int?, // Remove this nullability when session search changes to new endpoints
) {
  companion object
}
