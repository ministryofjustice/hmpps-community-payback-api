package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.FormCacheEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.FormCacheEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.scheduledjobs.ScheduledJobFormCacheExpiry
import java.time.OffsetDateTime

class ScheduledJobFormCacheExpiryIT : IntegrationTestBase() {

  @Autowired
  lateinit var job: ScheduledJobFormCacheExpiry

  @Autowired
  lateinit var repository: FormCacheEntityRepository

  @Test
  fun `remove entries last updated over 7 days ago`() {
    val retain1OnThreshold = repository.save(
      FormCacheEntity.valid().copy(
        updatedAt = OffsetDateTime.now().minusDays(6).minusHours(23),
      ),
    )

    val retain2ClearOfThreshold = repository.save(
      FormCacheEntity.valid().copy(
        updatedAt = OffsetDateTime.now(),
      ),
    )

    // just over threshold
    repository.save(
      FormCacheEntity.valid().copy(
        updatedAt = OffsetDateTime.now().minusDays(7).minusHours(1),
      ),
    )

    // well over threshold
    repository.save(
      FormCacheEntity.valid().copy(
        updatedAt = OffsetDateTime.now().minusDays(45),
      ),
    )

    job.removeExpiredFormData()

    val remainingEntries = repository.findAll()
    assertThat(remainingEntries).containsExactlyInAnyOrder(retain1OnThreshold, retain2ClearOfThreshold)
  }
}
