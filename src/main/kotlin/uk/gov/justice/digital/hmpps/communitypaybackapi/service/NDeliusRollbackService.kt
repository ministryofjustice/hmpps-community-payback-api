package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Scope
import org.springframework.context.annotation.ScopedProxyMode
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.web.context.WebApplicationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.NDeliusRollbackRequired
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.SentryService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.SpringEventPublisher

/**
 * If a request cannot be completed we want to rollback any changes made in NDelius. To do
 * this we wrap any internally raised [CommunityPaybackSpringEvent]s in an [NDeliusRollbackRequired]
 * event and publish them in the received order. The service responsible for making the original
 * change in NDelius can then revert the change made (i.e. apply a compensating transaction)
 *
 * This doesn't currently apply to application-scheduling, which is triggered by SQS message
 * consumer which doesn't have a request scope. This should be a solve-able problem.
 */
@Service
class NDeliusRollbackService(
  val springEventPublisher: SpringEventPublisher,
  val sentryService: SentryService,
  val applicationContext: ApplicationContext,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  @EventListener
  fun captureEvent(event: CommunityPaybackSpringEvent) {
    getRequestScopedEvents().add(event)
  }

  @SuppressWarnings("TooGenericExceptionCaught")
  fun publishEventsForRollback() {
    val events = getRequestScopedEvents().getAll()

    if (events.size > 50) {
      log.error("Unexpectedly large number of events (${events.size}) requested for rollback. Will not rollback. Events are $events")
      return
    }

    events.toList().forEach {
      try {
        log.info("Rolling back event $it")
        springEventPublisher.publishEvent(NDeliusRollbackRequired(it))
      } catch (e: Throwable) {
        log.error("Failed to rollback event $it", e)
        sentryService.captureException(e)
      }
    }
  }

  private fun getRequestScopedEvents() = applicationContext.getBean<RequestScopedEvents>()
}

@Component
@Scope(
  value = WebApplicationContext.SCOPE_REQUEST,
  proxyMode = ScopedProxyMode.TARGET_CLASS,
)
class RequestScopedEvents {
  val events = mutableListOf<CommunityPaybackSpringEvent>()

  fun add(event: CommunityPaybackSpringEvent) = events.add(event)

  fun getAll() = events.toList()
}
