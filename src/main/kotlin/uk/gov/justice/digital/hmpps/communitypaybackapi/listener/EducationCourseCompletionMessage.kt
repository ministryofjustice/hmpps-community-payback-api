package uk.gov.justice.digital.hmpps.communitypaybackapi.listener

import java.time.LocalDate
import java.time.OffsetDateTime

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
  val firstName: String,
  val lastName: String,
  val dateOfBirth: LocalDate,
  val region: String,
  val pdu: String,
  val office: String,
  val email: String,
  val courseName: String,
  val courseType: String,
  val provider: String,
  val completionDateTime: OffsetDateTime,
  val status: EducationCourseCompletionStatus,
  val totalTimeMinutes: Long,
  val attempts: Int?,
  val expectedTimeMinutes: Long,
  val externalReference: String,
) {
  companion object
}

enum class EducationCourseCompletionStatus {
  Completed,
  Failed,
}
