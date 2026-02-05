package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseCompletionMessage
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseCompletionStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseMessageAttributes
import kotlin.random.Random

fun EducationCourseCompletionMessage.Companion.valid() = EducationCourseCompletionMessage(
  messageId = String.random(),
  eventType = "educationCourseCompletionCreated",
  description = null,
  messageAttributes = EducationCourseMessageAttributes.valid(),
  who = null,
)

fun EducationCourseMessageAttributes.Companion.valid() = EducationCourseMessageAttributes(
  firstName = String.random(20),
  lastName = String.random(20),
  dateOfBirth = randomLocalDate(),
  region = String.random(10),
  email = "${String.random(10).lowercase()}@example.com",
  courseName = String.random(20),
  courseType = String.random(20),
  provider = String.random(5),
  status = EducationCourseCompletionStatus.entries.random(),
  completionDate = randomLocalDate(),
  totalTimeMinutes = Random.nextLong(10, 100),
  attempts = 1,
  expectedTimeMinutes = Random.nextLong(30, 240),
  externalReference = String.random(),
)
