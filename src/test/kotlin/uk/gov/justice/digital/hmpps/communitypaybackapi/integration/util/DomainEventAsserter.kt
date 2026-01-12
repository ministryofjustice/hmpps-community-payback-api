package uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util

import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import org.awaitility.Awaitility.await
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.test.context.event.annotation.BeforeTestMethod
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.HmppsDomainEvent
import java.util.concurrent.TimeUnit
import kotlin.collections.first
import kotlin.jvm.java

@Service
class DomainEventAsserter(private val objectMapper: ObjectMapper) {
  private val log = LoggerFactory.getLogger(this::class.java)

  private val messages = mutableListOf<HmppsDomainEvent>()

  @SqsListener(queueNames = ["hmppsdomaineventsasserterqueue"], factory = "hmppsQueueContainerFactoryProxy", pollTimeoutSeconds = "1")
  fun processMessage(rawMessage: String?) {
    val sqsMessage = objectMapper.readValue(rawMessage, SqsMessage::class.java)
    val event = objectMapper.readValue(sqsMessage.Message, HmppsDomainEvent::class.java)

    log.info("Received Domain Event: $event")
    synchronized(messages) {
      messages.add(event)
    }
  }

  @BeforeTestMethod
  fun clearMessages() = synchronized(messages) {
    messages.clear()
  }

  fun blockForDomainEventOfType(eventType: String): HmppsDomainEvent {
    await()
      .atMost(1, TimeUnit.SECONDS)
      .until { haveReceived(eventType) }

    synchronized(messages) {
      return messages.first { it.eventType == eventType }
    }
  }

  fun assertEventCount(eventType: String, count: Int) {
    await()
      .atMost(1, TimeUnit.SECONDS)
      .until { haveReceivedExactCount(eventType, count) }
  }

  private fun haveReceived(eventType: String): Boolean {
    synchronized(messages) {
      return messages.any { it.eventType == eventType }
    }
  }

  private fun haveReceivedExactCount(eventType: String, count: Int): Boolean {
    synchronized(messages) {
      return messages.filter { it.eventType == eventType }.size == count
    }
  }
}

// Warning suppressed because we have to match the SNS attribute naming case
@SuppressWarnings("ConstructorParameterNaming")
data class SqsMessage(
  val Message: String,
)
