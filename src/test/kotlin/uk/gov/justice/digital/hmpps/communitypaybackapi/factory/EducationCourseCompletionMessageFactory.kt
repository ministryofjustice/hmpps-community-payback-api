package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseCompletionMessage
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseCompletionStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseCourse
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCoursePerson
import kotlin.random.Random

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
  email = "${String.random(10).lowercase()}@example.com",
)

fun EducationCourseCourse.Companion.valid() = EducationCourseCourse(
  courseName = String.random(20),
  courseType = String.random(20),
  provider = String.random(5),
  status = EducationCourseCompletionStatus.entries.random(),
  totalTime = Random.nextLong(10, 100),
  expectedMinutes = Random.nextInt(30, 240),
)
