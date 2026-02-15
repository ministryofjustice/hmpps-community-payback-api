package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import java.util.UUID

data class CourseCompletionOutcomeDto(
  val crn: String,
  val appointmentIdToUpdate: UUID?,
  val minutesToCredit: Long,
  val contactOutcomeCode: String,
)
