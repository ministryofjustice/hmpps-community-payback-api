package uk.gov.justice.digital.hmpps.communitypaybackapi.bootstrap

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseEventCompletionMessageStatus
import java.time.LocalDate
import java.util.UUID
import kotlin.random.Random

@Component
@ConditionalOnProperty(name = ["community-payback.auto-seed.enabled"], havingValue = "true")
class DevEteCourseCompletionFixtures(
  private val repository: EteCourseCompletionEventEntityRepository,
) : AutoSeeder {
  private val log = LoggerFactory.getLogger(javaClass)
  val seededRandom = Random(SEED)

  override fun seed() {
    val fixtures = List(NUMBER_OF_RECORDS) { buildFixture(it) }

    fixtures.forEach { entity ->
      if (!repository.existsById(entity.id)) {
        repository.save(entity)
        log.info("[Dev Fixtures] Inserted ete_course_completion_event id={} externalRef={}", entity.id, entity.externalReference)
      } else {
        log.debug("[Dev Fixtures] Skipped existing ete_course_completion_event id={}", entity.id)
      }
    }
  }

  private fun buildFixtures(): List<EteCourseCompletionEventEntity> {
    val records = mutableListOf<EteCourseCompletionEventEntity>()
    for (i in 0 until NUMBER_OF_RECORDS) {
      records.add(buildFixture(i))
    }
    return records
  }

  private fun buildFixture(offset: Int): EteCourseCompletionEventEntity {
    val firstName = randFirstName()
    val lastName = randLastName()
    val totalTime = seededRandom.nextLong(3, 30) * 10
    return EteCourseCompletionEventEntity(
      id = UUID.fromString("e48bfee0-f6b5-4e64-a7a7-" + offset.toString().padStart(12, '0')),
      firstName = firstName,
      lastName = lastName,
      dateOfBirth = randDOB(),
      region = "East of England",
      email = "$firstName.$lastName@example.test",
      courseName = randomCourseName(),
      courseType = "Online",
      provider = "Megan Excess",
      completionDate = LocalDate.now().minusDays(seededRandom.nextLong(1, 8)),
      status = EteCourseEventCompletionMessageStatus.COMPLETED,
      totalTimeMinutes = totalTime,
      expectedTimeMinutes = totalTime + seededRandom.nextLong(-10, 10),
      attempts = 1,
      externalReference = "EXT-DEV-" + offset.toString().padStart(6, '0'),

    )
  }

  private fun randFirstName(): String {
    val names = listOf("Alex", "Jamie", "Taylor", "Jordan", "Morgan", "Casey", "Riley", "Drew", "Cameron", "Avery")
    return names[seededRandom.nextInt(names.count())]
  }

  private fun randLastName(): String {
    val names = listOf("Smith", "Johnson", "Brown", "Taylor", "Anderson", "Thomas", "Jackson", "White", "Harris")
    return names[seededRandom.nextInt(names.count())]
  }

  private fun randDOB(): LocalDate {
    val year = 1970 + seededRandom.nextInt(0, 31)
    return LocalDate.of(year, 1, 1).plusDays(seededRandom.nextLong(0, 365))
  }

  private fun randomCourseName(): String {
    val courses = listOf("Construction Skills Level 1", "Health and Safety Basics", "Food Hygiene Level 2", "Customer Service Excellence", "IT Fundamentals", "Business Communication Skills")
    return courses[seededRandom.nextInt(courses.count())]
  }

  companion object {
    const val NUMBER_OF_RECORDS = 50
    const val SEED = 12345L
  }
}
