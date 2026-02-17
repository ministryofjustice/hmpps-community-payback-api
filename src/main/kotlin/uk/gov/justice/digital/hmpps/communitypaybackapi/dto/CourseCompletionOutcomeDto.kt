package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

data class CourseCompletionOutcomeDto(
  val crn: String,
  val appointmentIdToUpdate: Long?,
  val minutesToCredit: Long,
  val contactOutcome: String,
  val projectCode: String,
) {
  companion object
}
