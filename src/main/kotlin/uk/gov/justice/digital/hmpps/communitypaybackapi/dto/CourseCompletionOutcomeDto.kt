package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

data class CourseCompletionOutcomeDto(
  val crn: String,
  val deliusEventNumber: Long,
  val appointmentIdToUpdate: Long?,
  val minutesToCredit: Long,
  val contactOutcomeCode: String,
  val projectCode: String,
) {
  companion object
}
