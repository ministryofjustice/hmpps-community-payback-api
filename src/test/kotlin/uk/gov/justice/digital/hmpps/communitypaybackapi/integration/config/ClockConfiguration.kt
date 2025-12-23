package uk.gov.justice.digital.hmpps.communitypaybackapi.integration.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.test.context.event.annotation.BeforeTestMethod
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

@Configuration
class ClockConfiguration {

  val clock: MutableClock
    @Bean
    @Primary
    get() = MutableClock()

  @BeforeTestMethod
  fun reset() {
    clock.reset()
  }

  open class MutableClock(time: Instant? = null) : Clock() {
    var fixedTime: Instant? = time

    fun reset() {
      fixedTime = null
    }

    fun setNow(now: LocalDateTime) {
      fixedTime = now.toInstant(ZoneOffset.UTC)
    }

    override fun instant(): Instant = fixedTime ?: Instant.now()

    override fun withZone(zone: ZoneId?): Clock {
      error("Not supported")
    }

    override fun getZone(): ZoneId = ZoneId.systemDefault()
  }
}
