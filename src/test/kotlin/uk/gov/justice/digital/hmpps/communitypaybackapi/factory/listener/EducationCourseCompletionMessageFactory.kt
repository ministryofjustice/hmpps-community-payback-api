package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.listener

import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.CommunityCampusPduEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalDate
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomOffsetDateTime
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseCompletionMessage
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseCompletionStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseMessageAttributes
import kotlin.random.Random

fun EducationCourseCompletionMessage.Companion.valid() = EducationCourseCompletionMessage(
  messageId = String.Companion.random(),
  eventType = "educationCourseCompletionCreated",
  description = null,
  messageAttributes = EducationCourseMessageAttributes.valid(),
  who = null,
)

fun EducationCourseCompletionMessage.Companion.valid(ctx: ApplicationContext) = EducationCourseCompletionMessage.valid().copy(
  messageAttributes = EducationCourseMessageAttributes.valid(ctx),
)

fun EducationCourseMessageAttributes.Companion.valid() = EducationCourseMessageAttributes(
  firstName = String.random(20),
  lastName = String.random(20),
  dateOfBirth = randomLocalDate(),
  region = String.random(10),
  pdu = String.random(20),
  office = String.random(20),
  email = "${String.random(10).lowercase()}@example.com",
  courseName = String.random(20),
  courseType = String.random(20),
  provider = String.random(5),
  status = EducationCourseCompletionStatus.entries.random(),
  completionDateTime = randomOffsetDateTime(),
  totalTimeMinutes = Random.nextLong(10, 100),
  attempts = 1,
  expectedTimeMinutes = Random.nextLong(30, 240),
  externalReference = String.random(),
)

fun EducationCourseMessageAttributes.Companion.valid(ctx: ApplicationContext) = EducationCourseMessageAttributes.valid().copy(
  pdu = ctx.getBean<CommunityCampusPduEntityRepository>().findAll().minByOrNull { it.name }!!.name,
)
