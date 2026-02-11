package uk.gov.justice.digital.hmpps.communitypaybackapi.bootstrap

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.EnvironmentService

@Service
@ConditionalOnProperty(name = ["community-payback.auto-seed.enabled"], havingValue = "true")
class AutoSeedService(
  private val environmentService: EnvironmentService,
  private val seeders: List<AutoSeeder> = listOf(),
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  @Suppress("TooGenericExceptionCaught")
  @PostConstruct
  fun onStartup() {
    if (environmentService.isNotATestEnvironment()) {
      error("auto seed should not be enabled outside of test environments")
    }

    if (seeders.isEmpty()) {
      log.info("[AutoSeed] No seeders registered")
    } else {
      log.info("[AutoSeed] Running ${seeders.size} seeders...")
      seeders.forEach { seeder ->
        try {
          seeder.seed()
          log.info("[AutoSeed] Seeder ${seeder::class.simpleName} completed")
        } catch (e: Exception) {
          log.error("[AutoSeed] Seeder ${seeder::class.simpleName} failed", e)
          throw e
        }
      }
      log.info("[AutoSeed] Completed running seeders")
    }
  }
}
