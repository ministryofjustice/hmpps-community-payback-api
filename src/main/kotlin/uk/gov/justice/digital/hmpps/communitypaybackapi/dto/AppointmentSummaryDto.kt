package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import io.swagger.v3.oas.annotations.media.Schema

data class AppointmentSummaryDto(
  val id: Long,
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
