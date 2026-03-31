package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import java.time.LocalDate

data class CourseCompletionResolutionDto(
  val type: CourseCompletionResolutionTypeDto,
  val crn: String?,
  @param:Schema(description = "Must be provided if type is 'CREDIT_TIME'")
  val creditTimeDetails: CourseCompletionCreditTimeDetailsDto?,
  @param:Schema(description = "Must be provided if type is 'DONT_CREDIT_TIME'")
  val dontCreditTimeDetails: CourseCompletionDontCreditTimeDetailsDto?,
) {
  companion object
}

data class CourseCompletionCreditTimeDetailsDto(
  val deliusEventNumber: Int,
  val appointmentIdToUpdate: Long?,
  val date: LocalDate,
  @field:Min(value = 1)
  val minutesToCredit: Long,
  val contactOutcomeCode: String,
  val projectCode: String,
  val notes: String?,
  val alertActive: Boolean?,
  val sensitive: Boolean?,
) {
  companion object
}

data class CourseCompletionDontCreditTimeDetailsDto(
  val notes: String?,
) {
  companion object
}

enum class CourseCompletionResolutionTypeDto {
  CREDIT_TIME,
  DONT_CREDIT_TIME,
}
