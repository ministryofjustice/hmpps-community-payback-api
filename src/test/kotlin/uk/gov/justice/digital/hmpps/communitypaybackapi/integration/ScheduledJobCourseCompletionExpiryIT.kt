package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionDraftResolutionEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionDraftResolutionRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventResolutionEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventResolutionRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.scheduledjobs.ScheduledJobCourseCompletionExpiry
import java.time.OffsetDateTime

class ScheduledJobCourseCompletionExpiryIT : IntegrationTestBase() {
  @Autowired
  lateinit var job: ScheduledJobCourseCompletionExpiry

  @Autowired
  lateinit var courseCompletionEventEntityRepository: EteCourseCompletionEventEntityRepository

  @Autowired
  lateinit var draftResolutionRepository: EteCourseCompletionDraftResolutionRepository

  @Autowired
  lateinit var courseCompletionResolutionRepository: EteCourseCompletionEventResolutionRepository

  private fun assertThatCourseCompletionIsAbsent(courseCompletion: EteCourseCompletionEventEntity) {
    assertThat(courseCompletionEventEntityRepository.findAll().map { it.id }).doesNotContain(courseCompletion.id)
  }

  private fun assertThatCourseCompletionIsPresent(courseCompletion: EteCourseCompletionEventEntity) {
    assertThat(courseCompletionEventEntityRepository.findAll().map { it.id }).contains(courseCompletion.id)
  }

  private fun assertThatDraftResolutionIsAbsent(draftResolution: EteCourseCompletionDraftResolutionEntity) {
    assertThat(draftResolutionRepository.findAll().map { it.id }).doesNotContain(draftResolution.id)
  }

  private fun assertThatDraftResolutionIsPresent(draftResolution: EteCourseCompletionDraftResolutionEntity) {
    assertThat(draftResolutionRepository.findAll().map { it.id }).contains(draftResolution.id)
  }

  @Test
  fun `removes unresolved course completion events received over 7 days ago`() {
    val courseCompletion = EteCourseCompletionEventEntity.valid(ctx).copy(receivedAt = OffsetDateTime.now().minusDays(7).minusSeconds(1))
    courseCompletionEventEntityRepository.save(courseCompletion)

    job.removeExpiredCourseCompletions()

    assertThatCourseCompletionIsAbsent(courseCompletion)
  }

  @Test
  fun `removes draft resolutions for unresolved course completion events received over 7 days ago`() {
    val courseCompletion = EteCourseCompletionEventEntity.valid(ctx).copy(receivedAt = OffsetDateTime.now().minusDays(7).minusSeconds(1))
    courseCompletionEventEntityRepository.save(courseCompletion)

    val draftResolution = EteCourseCompletionDraftResolutionEntity.valid().copy(eteCourseCompletionEvent = courseCompletion)
    draftResolutionRepository.save(draftResolution)

    job.removeExpiredCourseCompletions()

    assertThatDraftResolutionIsAbsent(draftResolution)
  }

  @Test
  fun `does not remove resolved course completion events received over 7 days ago`() {
    val courseCompletion = EteCourseCompletionEventEntity.valid(ctx).copy(receivedAt = OffsetDateTime.now().minusDays(7).minusSeconds(1))
    courseCompletionEventEntityRepository.save(courseCompletion)

    val resolution = EteCourseCompletionEventResolutionEntity.valid(ctx).copy(eteCourseCompletionEvent = courseCompletion)
    courseCompletionResolutionRepository.save(resolution)

    job.removeExpiredCourseCompletions()

    assertThatCourseCompletionIsPresent(courseCompletion)
  }

  @Test
  fun `does not remove draft resolutions for resolved course completion events received over 7 days ago`() {
    val courseCompletion = EteCourseCompletionEventEntity.valid(ctx).copy(receivedAt = OffsetDateTime.now().minusDays(7).minusSeconds(1))
    courseCompletionEventEntityRepository.save(courseCompletion)

    val resolution = EteCourseCompletionEventResolutionEntity.valid(ctx).copy(eteCourseCompletionEvent = courseCompletion)
    courseCompletionResolutionRepository.save(resolution)

    val draftResolution = EteCourseCompletionDraftResolutionEntity.valid().copy(eteCourseCompletionEvent = courseCompletion)
    draftResolutionRepository.save(draftResolution)

    job.removeExpiredCourseCompletions()

    assertThatDraftResolutionIsPresent(draftResolution)
  }

  @Test
  fun `does not remove unresolved course completion events received less than 7 days ago`() {
    val courseCompletion = EteCourseCompletionEventEntity.valid(ctx).copy(receivedAt = OffsetDateTime.now().minusDays(7).plusSeconds(1))
    courseCompletionEventEntityRepository.save(courseCompletion)

    job.removeExpiredCourseCompletions()

    assertThatCourseCompletionIsPresent(courseCompletion)
  }

  @Test
  fun `does not remove draft resolutions for unresolved course completion events received less than 7 days ago`() {
    val courseCompletion = EteCourseCompletionEventEntity.valid(ctx).copy(receivedAt = OffsetDateTime.now().minusDays(7).plusSeconds(1))
    courseCompletionEventEntityRepository.save(courseCompletion)

    val draftResolution = EteCourseCompletionDraftResolutionEntity.valid().copy(eteCourseCompletionEvent = courseCompletion)
    draftResolutionRepository.save(draftResolution)

    job.removeExpiredCourseCompletions()

    assertThatDraftResolutionIsPresent(draftResolution)
  }
}
