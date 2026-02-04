package uk.gov.justice.digital.hmpps.communitypaybackapi.listener

import java.time.LocalDate
import java.time.LocalDateTime

data class EducationCourseCompletionMessage(
  val externalReference: String,
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
  val courseType: String,
  val provider: String,
  val completionDateTime: LocalDateTime,
  val status: EducationCourseCompletionStatus,
  val totalTime: Long,
  val expectedMinutes: Int,
) {
  companion object
}

enum class EducationCourseCompletionStatus {
  Completed,
  Failed,
}
