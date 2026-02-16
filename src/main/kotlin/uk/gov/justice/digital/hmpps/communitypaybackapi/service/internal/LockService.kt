package uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal

import org.redisson.api.RedissonClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.TimeUnit

interface LockService {
  fun <T> withDistributedLock(
    key: String,
    waitTime: Duration = Duration.ofSeconds(60),
    leaseTime: Duration,
    exec: () -> T,
  ): T
}

@Component
class RedisLockService(
  private val redissonClient: RedissonClient,
) : LockService {
  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  override fun <T> withDistributedLock(
    key: String,
    waitTime: Duration,
    leaseTime: Duration,
    exec: () -> T,
  ): T {
    val lock = redissonClient.getLock(key)
    if (lock.tryLock(waitTime.seconds, leaseTime.seconds, TimeUnit.SECONDS)) {
      try {
        log.trace("Have acquired lock '{}' with lease '{}'", key, leaseTime)
        return exec.invoke()
      } finally {
        lock.unlock()
        log.trace("Have unlocked '{}'", key)
      }
    } else {
      error("Could not acquire lock '$key' after $waitTime'")
    }
  }
}
