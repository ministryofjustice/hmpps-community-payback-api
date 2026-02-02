package uk.gov.justice.digital.hmpps.communitypaybackapi.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.messaging.MessageHeaders
import org.springframework.messaging.handler.annotation.Headers
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdditionalInformationType
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.DomainEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingAppointmentDomainEventHandler
import java.time.Duration
import java.util.UUID

/**
 * The type of domain events this listener receives are managed by the cloud platform configuration
 * See https://tech-docs.hmpps.service.justice.gov.uk/common-kotlin-patterns/domain-events/
 */
@Service
@ConditionalOnProperty(name = ["community-payback.scheduling.enabled"], havingValue = "true")
class DomainEventListener(
  private val objectMapper: ObjectMapper,
  private val sqsListenerErrorHandler: SqsListenerErrorHandler,
  private val schedulingAppointmentEventHandler: SchedulingAppointmentDomainEventHandler,
) {
  private companion object {
    /**
     * Message visibility should be set according to the longest possible processing
     * time for a domain event.
     *
     * For appointment changes, the slowest possible processing time is determined
     * by the timeouts of upstream calls, as internal processing should be fast:
     *
     * 1. get non-working days via cp-and-delius (5 seconds timeout)
     * 2. get unpaid work requirement via cp-and-delius (5 seconds timeout)
     * 3. bulk create appointments via cp-and-delius (5 seconds timeout)
     *
     * 30 seconds should be more than enough time for this
     */
    const val MESSAGE_VISIBILITY_TIMEOUT: Long = 30L

    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @SqsListener(
    value = ["hmppsdomaineventsqueue"],
    factory = "hmppsQueueContainerFactoryProxy",
    messageVisibilitySeconds = MESSAGE_VISIBILITY_TIMEOUT.toString(),
    pollTimeoutSeconds = "\${hmpps.sqs.pollTimeoutSeconds:}",
  )
  fun domainEvent(
    messageString: String,
    @Headers headers: MessageHeaders,
  ) {
    sqsListenerErrorHandler.withErrorHandler(headers) {
      handleDomainEvent(messageString)
    }
  }

  private fun handleDomainEvent(messageString: String) {
    log.debug("Have received domain event message '$messageString'")

    val sqsMessage = objectMapper.readValue<SqsMessage>(messageString)
    val event = objectMapper.readValue<HmppsDomainEvent>(sqsMessage.message)

    when (event.eventType) {
      DomainEventType.APPOINTMENT_UPDATED.eventType,
      DomainEventType.APPOINTMENT_CREATED.eventType,
      -> schedulingAppointmentEventHandler.handleAppointmentEvent(
        eventId = event.getEventId(),
        maxProcessingTime = Duration.ofSeconds(MESSAGE_VISIBILITY_TIMEOUT),
      )
      else -> log.warn("Unexpected event type '${event.eventType}'")
    }
  }

  private fun HmppsDomainEvent.getEventId() = additionalInformation?.map[AdditionalInformationType.EVENT_ID.name]?.toString()?.let {
    UUID.fromString(it)
  } ?: error("Can't find event id")
}
