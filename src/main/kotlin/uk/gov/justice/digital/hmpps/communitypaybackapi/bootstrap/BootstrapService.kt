package uk.gov.justice.digital.hmpps.communitypaybackapi.bootstrap

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.EnvironmentService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.LockService
import java.time.Duration

@Service
@ConditionalOnProperty(name = ["community-payback.bootstrap.enabled"], havingValue = "true")
class BootstrapService(
  private val environmentService: EnvironmentService,
  private val seeders: List<AutoSeeder> = listOf(),
  private val lockService: LockService,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  @PostConstruct
  fun onStartup() {
    if (environmentService.isNotATestEnvironment()) {
      error("boot strap should not be enabled outside of test environments")
    }

    lockService.withDistributedLock(key = "bootstrap", leaseTime = Duration.ofSeconds(10)) {
      runAutoSeeders()
    }
  }

  @Suppress("TooGenericExceptionCaught")
  private fun runAutoSeeders() {
    if (seeders.isEmpty()) {
      log.info("No auto-seeders registered")
    } else {
      log.info("Running ${seeders.size} seeders...")
      seeders.forEach { seeder ->
        try {
          seeder.seed()
          log.info("Seeder ${seeder::class.simpleName} completed")
        } catch (e: Exception) {
          log.error("Seeder ${seeder::class.simpleName} failed", e)
          throw e
        }
      }
      log.info("Completed running seeders")
    }
  }
}
