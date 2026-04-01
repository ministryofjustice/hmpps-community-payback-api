package uk.gov.justice.digital.hmpps.communitypaybackapi.integration.config

import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import org.springframework.test.context.event.annotation.BeforeTestMethod
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.DomainEventListenerConfig

/**
 * We want to disable scheduling by default during integration tests because it can
 * create lots of noise in the logs, including errors. This service allows the domain
 * event listener to be selectively enabled on-the-fly without having to reconfigure
 * spring and create a new application context, which will slow down tests
 */
@Service
@Primary
class MutableDomainEventListenerConfig : DomainEventListenerConfig {

  var enabled: Boolean = false

  override fun enabled(): Boolean = enabled

  fun enable() {
    enabled = true
  }

  @BeforeTestMethod
  fun reset() {
    enabled = false
  }
}
