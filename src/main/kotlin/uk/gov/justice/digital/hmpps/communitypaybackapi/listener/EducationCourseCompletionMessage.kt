package uk.gov.justice.digital.hmpps.communitypaybackapi.listener

import java.time.LocalDate
import java.time.LocalDateTime

data class EducationCourseCompletionMessage(
  val messageId: String,
  val eventType: String,
  val description: String?,
  val messageAttributes: EducationCourseMessageAttributes,
  val who: String?,
) {
  companion object
}

data class EducationCourseMessageAttributes(
  val crn: String,
  val firstName: String,
  val lastName: String,
  val dateOfBirth: LocalDate,
  val region: String,
  val email: String,
  val courseName: String,
  val courseType: String,
  val provider: String,
  val completionDateTime: LocalDateTime,
  val status: EducationCourseCompletionStatus,
  val totalTime: Long,
  val attempts: String?,
  val expectedMinutes: Int,
  val externalReference: String,
) {
  companion object
}

enum class EducationCourseCompletionStatus {
  Completed,
  Failed,
}
