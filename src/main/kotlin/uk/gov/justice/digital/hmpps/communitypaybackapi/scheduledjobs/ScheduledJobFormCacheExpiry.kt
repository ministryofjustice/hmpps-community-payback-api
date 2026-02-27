package uk.gov.justice.digital.hmpps.communitypaybackapi.scheduledjobs

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.FormService

@Component
class ScheduledJobFormCacheExpiry(
  private val formService: FormService,
) {
  @Scheduled(cron = "0 0 0/2 * * *")
  @SchedulerLock(
    name = "form_cache_expiry",
    lockAtMostFor = "1m",
    lockAtLeastFor = "1m",
  )
  fun removeExpiredFormData() {
    formService.deleteExpiredEntries()
  }
}
