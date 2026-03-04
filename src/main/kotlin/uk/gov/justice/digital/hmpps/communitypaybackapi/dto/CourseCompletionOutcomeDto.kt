package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import jakarta.validation.constraints.Min

data class CourseCompletionOutcomeDto(
  val crn: String,
  val deliusEventNumber: Long,
  val appointmentIdToUpdate: Long?,
  @field:Min(value = 1)
  val minutesToCredit: Long,
  val contactOutcomeCode: String,
  val projectCode: String,
) {
  companion object
}
