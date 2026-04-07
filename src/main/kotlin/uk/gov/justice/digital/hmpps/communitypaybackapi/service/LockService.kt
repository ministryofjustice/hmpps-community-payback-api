package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.redisson.api.RBucket
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

  fun singleFlightForIntResult(
    lockKey: String,
    resultKey: String,
    lockWaitTime: Duration,
    lockLeaseTime: Duration,
    exec: () -> Int,
  ): Int
}

@Component
class RedisLockService(
  private val redissonClient: RedissonClient,
) : LockService {
  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  /**
   * key should ideally be named `lock:{group}:{value}
   */
  override fun <T> withDistributedLock(
    key: String,
    waitTime: Duration,
    leaseTime: Duration,
    exec: () -> T,
  ): T {
    val lock = redissonClient.getLock(key)
    log.trace("Acquiring lock '{}' with lease '{}'", key, leaseTime)
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

  override fun singleFlightForIntResult(
    lockKey: String,
    resultKey: String,
    lockWaitTime: Duration,
    lockLeaseTime: Duration,
    exec: () -> Int,
  ): Int {
    val bucket: RBucket<Int> = redissonClient.getBucket(resultKey)
    bucket.get()?.let {
      log.info("Using existing result $it for key $resultKey")
      return it
    }

    return withDistributedLock(
      key = lockKey,
      waitTime = lockWaitTime,
      leaseTime = lockLeaseTime,
    ) {
      bucket.get()?.let {
        log.info("Using existing result $it after acquiring lock for key $resultKey")
        return@withDistributedLock it
      }

      val result = exec()
      bucket.set(result, Duration.ofDays(7))

      result
    }
  }
}
