package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.CommunityCampusCourse
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.CommunityCampusCourseCompletionMessage
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.CommunityCampusCourseCompletionStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.CommunityCampusPerson
import java.time.LocalDateTime
import kotlin.String

fun CommunityCampusCourseCompletionMessage.Companion.valid() = CommunityCampusCourseCompletionMessage(
  communityCampusId = String.random(),
  person = CommunityCampusPerson.valid(),
  course = CommunityCampusCourse.valid(),
)

fun CommunityCampusPerson.Companion.valid() = CommunityCampusPerson(
  crn = String.random(5),
  firstName = String.random(20),
  lastName = String.random(20),
  dateOfBirth = randomLocalDate(),
  region = String.random(10),
  email = String.random(20),
)

fun CommunityCampusCourse.Companion.valid() = CommunityCampusCourse(
  courseName = String.random(20),
  source = String.random(5),
  enrollmentDateTime = LocalDateTime.now().minusDays(10),
  completionDateTime = LocalDateTime.now().minusDays(5),
  status = CommunityCampusCourseCompletionStatus.entries.random(),
  totalTime = randomHourMinuteDuration(),
  attempts = Int.random(0, 10),
  expectedMinutes = Int.random(0, 10),
  expectedMinutesAdditional = Int.random(0, 5),
)
