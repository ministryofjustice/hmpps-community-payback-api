package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseCompletionMessage
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseCompletionStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseCourse
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCoursePerson
import java.time.LocalDateTime
import kotlin.String

fun EducationCourseCompletionMessage.Companion.valid() = EducationCourseCompletionMessage(
  externalId = String.random(),
  person = EducationCoursePerson.valid(),
  course = EducationCourseCourse.valid(),
)

fun EducationCoursePerson.Companion.valid() = EducationCoursePerson(
  crn = String.random(5),
  firstName = String.random(20),
  lastName = String.random(20),
  dateOfBirth = randomLocalDate(),
  region = String.random(10),
  email = String.random(20),
)

fun EducationCourseCourse.Companion.valid() = EducationCourseCourse(
  courseName = String.random(20),
  source = String.random(5),
  enrollmentDateTime = LocalDateTime.now().minusDays(10),
  completionDateTime = LocalDateTime.now().minusDays(5),
  status = EducationCourseCompletionStatus.entries.random(),
  totalTime = randomHourMinuteDuration(),
  attempts = Int.random(0, 10),
  expectedMinutes = Int.random(0, 10),
  expectedMinutesAdditional = Int.random(0, 5),
)
