package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalTime

data class AppointmentSummaryDto(
  val id: Long,
  @param:Schema(example = "09:00", pattern = "^([0-1][0-9]|2[0-3]):[0-5][0-9]$")
  val startTime: LocalTime,
  @param:Schema(example = "17:00", pattern = "^([0-1][0-9]|2[0-3]):[0-5][0-9]$")
  val endTime: LocalTime,
  @param:Schema(
    description = "How many community payback minutes the offender is required to complete",
    example = "2400",
  )
  val contactOutcome: ContactOutcomeDto?,
  val requirementMinutes: Int,
  @param:Schema(description = "How many community payback minutes the offender has completed to date", example = "480")
  val completedMinutes: Int,
  val offender: OffenderDto,
) {
  companion object
}
