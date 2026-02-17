package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseEventCompletionMessageStatus
import java.time.LocalDate
import java.util.UUID
import kotlin.random.Random

fun EteCourseCompletionEventEntity.Companion.valid() = EteCourseCompletionEventEntity(
  id = UUID.randomUUID(),
  firstName = String.random(20),
  lastName = String.random(20),
  dateOfBirth = randomLocalDate(),
  region = String.random(10),
  email = "${String.random(10).lowercase()}@example.com",
  courseName = String.random(20),
  courseType = String.random(20),
  provider = String.random(5),
  status = EteCourseEventCompletionMessageStatus.entries.random(),
  completionDate = LocalDate.now().minusDays(Long.random(1, 30)),
  totalTimeMinutes = Random.nextLong(10, 100),
  attempts = 1,
  expectedTimeMinutes = Random.nextLong(30, 240),
  externalReference = String.random(),
)
