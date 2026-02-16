package uk.gov.justice.digital.hmpps.communitypaybackapi.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.CacheManager
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.cache.support.NoOpCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

/**
 * Configure a basic in-memory cache using Caffeine.
 *
 * Because we provide the [CacheManager] implementation, the `spring.cache.type` config is ignored. For this
 * reason we have to use our won config to disable the cache.
 *
 * Cache information is available from the 'caches' and 'metrics' actuator endpoints, which are only enabled
 * for local deployments (e.g. localhost:8080/caches). A valid token is required to access these endpoints.
 * Only pre-configured caches will appear on the metrics endpoint
 */
@SuppressWarnings("MagicNumber")
@Configuration
class CacheConfig {

  companion object {
    object CacheKey {
      object Delius {
        const val GET_PROJECT = "cpAndDelius.getProject"
        const val GET_PROVIDERS = "cpAndDelius.getProviders"
        const val GET_PROVIDER_TEAMS = "cpAndDelius.getProviderTeams"
        const val GET_SUPERVISORS = "cpAndDelius.getSupervisors"
        const val GET_TEAM_SUPERVISORS = "cpAndDelius.getTeamSupervisors"
        const val GET_NON_WORKING_DAYS = "cpAndDelius.getNonWorkingDays"
      }
    }
  }

  @Bean
  fun cacheManager(
    @Value("\${community-payback.cache.enabled:true}") enabled: Boolean,
  ): CacheManager {
    if (!enabled) {
      return NoOpCacheManager()
    }

    val caffeineCacheManager = CaffeineCacheManager()

    // default config
    caffeineCacheManager.setCaffeine(builder().expireAfterWrite(Duration.ofSeconds(30)))

    // cache-specific config
    caffeineCacheManager.registerCustomCache(CacheKey.Delius.GET_NON_WORKING_DAYS, builder().expireAfterWrite(Duration.ofHours(1)).build())
    caffeineCacheManager.registerCustomCache(CacheKey.Delius.GET_PROJECT, builder().expireAfterWrite(Duration.ofSeconds(30)).build())
    caffeineCacheManager.registerCustomCache(CacheKey.Delius.GET_PROVIDERS, builder().expireAfterWrite(Duration.ofMinutes(10)).build())
    caffeineCacheManager.registerCustomCache(CacheKey.Delius.GET_PROVIDER_TEAMS, builder().expireAfterWrite(Duration.ofMinutes(5)).build())
    caffeineCacheManager.registerCustomCache(CacheKey.Delius.GET_SUPERVISORS, builder().expireAfterWrite(Duration.ofMinutes(5)).build())
    caffeineCacheManager.registerCustomCache(CacheKey.Delius.GET_TEAM_SUPERVISORS, builder().expireAfterWrite(Duration.ofMinutes(5)).build())

    return caffeineCacheManager
  }

  private fun builder() = Caffeine.newBuilder().recordStats()
}
