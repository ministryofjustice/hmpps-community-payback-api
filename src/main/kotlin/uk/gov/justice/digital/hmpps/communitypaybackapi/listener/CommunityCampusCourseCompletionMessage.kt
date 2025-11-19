package uk.gov.justice.digital.hmpps.communitypaybackapi.listener

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.HourMinuteDuration
import java.time.LocalDate
import java.time.LocalDateTime

data class CommunityCampusCourseCompletionMessage(
  val communityCampusId: String,
  val person: CommunityCampusPerson,
  val course: CommunityCampusCourse,
) {
  companion object
}

data class CommunityCampusPerson(
  val crn: String,
  val firstName: String,
  val lastName: String,
  val dateOfBirth: LocalDate,
  // pending enumeration
  val region: String,
  val email: String,
)

data class CommunityCampusCourse(
  val courseName: String,
  val source: String,
  val enrollmentDateTime: LocalDateTime,
  val completionDateTime: LocalDateTime,
  val status: CommunityCampusCourseCompletionStatus,
  val totalTime: HourMinuteDuration,
  val attempts: Int?,
  val expectedMinutes: Int,
  val expectedMinutesAdditional: Int,
)

enum class CommunityCampusCourseCompletionStatus {
  Completed,
  Failed,
}
