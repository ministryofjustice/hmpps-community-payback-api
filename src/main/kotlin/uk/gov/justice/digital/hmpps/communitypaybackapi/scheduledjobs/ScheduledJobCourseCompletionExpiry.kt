package uk.gov.justice.digital.hmpps.communitypaybackapi.scheduledjobs

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionDraftResolutionRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntityRepository
import java.time.OffsetDateTime

@Component
class ScheduledJobCourseCompletionExpiry(
  private val courseCompletionEventEntityRepository: EteCourseCompletionEventEntityRepository,
  private val draftResolutionRepository: EteCourseCompletionDraftResolutionRepository,
) {
  companion object {
    private val log = LoggerFactory.getLogger(ScheduledJobCourseCompletionExpiry::class.java)

    private const val TTL_DAYS: Long = 7
  }

  @Scheduled(cron = "0 0 0/2 * * *")
  @SchedulerLock(
    name = "course_completion_expiry",
    lockAtMostFor = "1m",
    lockAtLeastFor = "1m",
  )
  @Transactional
  fun removeExpiredCourseCompletions() {
    val timestamp = OffsetDateTime.now().minusDays(TTL_DAYS)
    log.info("Removing unresolved course completions that were received before {} ({} days ago)", timestamp, TTL_DAYS)

    val courseCompletions = courseCompletionEventEntityRepository.getUnresolvedCourseCompletionsBefore(timestamp)
    log.info("Found {} unresolved course completion events", courseCompletions.size)

    if (courseCompletions.isEmpty()) {
      log.info("No unresolved course completion events to remove.")
      return
    }

    val draftResolutions = draftResolutionRepository.findByEteCourseCompletionEventIdIn(courseCompletions.map { it.id })
    log.info("Found {} draft resolutions", draftResolutions.size)

    draftResolutionRepository.deleteAllInBatch(draftResolutions)
    log.info("Draft resolutions removed")
    courseCompletionEventEntityRepository.deleteAllInBatch(courseCompletions)
    log.info("Course completion events removed")
  }
}
