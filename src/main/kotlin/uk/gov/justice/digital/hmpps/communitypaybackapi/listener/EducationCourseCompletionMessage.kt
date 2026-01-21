package uk.gov.justice.digital.hmpps.communitypaybackapi.listener

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.HourMinuteDuration
import java.time.LocalDate
import java.time.LocalDateTime

data class EducationCourseCompletionMessage(
  val externalId: String,
  val person: EducationCoursePerson,
  val course: EducationCourseCourse,
) {
  companion object
}

data class EducationCoursePerson(
  val crn: String,
  val firstName: String,
  val lastName: String,
  val dateOfBirth: LocalDate,
  // pending enumeration
  val region: String,
  val email: String,
) {
  companion object
}

data class EducationCourseCourse(
  val courseName: String,
  val source: String,
  val enrollmentDateTime: LocalDateTime,
  val completionDateTime: LocalDateTime,
  val status: EducationCourseCompletionStatus,
  val totalTime: HourMinuteDuration,
  val attempts: Int,
  val expectedMinutes: Int,
  val expectedMinutesAdditional: Int,
) {
  companion object
}

enum class EducationCourseCompletionStatus {
  Completed,
  Failed,
}
