package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity

import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.CommunityCampusPduEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.CommunityCampusPduEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalDate
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomOffsetDateTime
import java.util.UUID
import kotlin.random.Random

fun EteCourseCompletionEventEntity.Companion.valid() = EteCourseCompletionEventEntity(
  id = UUID.randomUUID(),
  firstName = String.random(20),
  lastName = String.random(20),
  dateOfBirth = randomLocalDate(),
  region = String.random(10),
  pdu = CommunityCampusPduEntity.valid(),
  office = String.random(20),
  email = "${String.random(10).lowercase()}@example.com",
  courseName = String.random(20),
  courseType = String.random(20),
  provider = String.random(5),
  status = EteCourseCompletionEventStatus.entries.random(),
  completionDateTime = randomOffsetDateTime(),
  totalTimeMinutes = Random.nextLong(10, 100),
  attempts = 1,
  expectedTimeMinutes = Random.nextLong(30, 240),
  externalReference = String.random(),
  receivedAt = randomOffsetDateTime(),
)

fun EteCourseCompletionEventEntity.Companion.valid(ctx: ApplicationContext) = EteCourseCompletionEventEntity.valid().copy(
  pdu = ctx.getBean<CommunityCampusPduEntityRepository>().findAll().minByOrNull { it.name }!!,
)

fun EteCourseCompletionEventEntity.Companion.failed(ctx: ApplicationContext) = EteCourseCompletionEventEntity.valid(ctx).copy(
  status = EteCourseCompletionEventStatus.FAILED,
)

fun EteCourseCompletionEventEntity.Companion.passed(ctx: ApplicationContext) = EteCourseCompletionEventEntity.valid(ctx).copy(
  status = EteCourseCompletionEventStatus.PASSED,
)
