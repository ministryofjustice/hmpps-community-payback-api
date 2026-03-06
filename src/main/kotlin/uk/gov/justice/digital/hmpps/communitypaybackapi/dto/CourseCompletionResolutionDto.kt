package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class CourseCompletionResolutionDto(
  val type: CourseCompletionResolutionTypeDto,
  val crn: String,
  @param:Schema(description = "Must be provided if type is 'CREDIT_TIME'")
  val creditTimeDetails: CourseCompletionCreditTimeDetailsDto?,
) {
  companion object
}

data class CourseCompletionCreditTimeDetailsDto(
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

enum class CourseCompletionResolutionTypeDto {
  CREDIT_TIME,
  COURSE_ALREADY_COMPLETED_WITHIN_THRESHOLD,
}
