package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import java.time.LocalDate

data class CourseCompletionResolutionDto(
  val crn: String,
  val deliusEventNumber: Long,
  val appointmentIdToUpdate: Long?,
  val date: LocalDate,
  val minutesToCredit: Long,
  val contactOutcomeCode: String,
  val projectCode: String,
  val notes: String?,
  val alertActive: Boolean?,
  val sensitive: Boolean?,
) {
  companion object
}
