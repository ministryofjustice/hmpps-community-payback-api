package uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.publish
import java.time.OffsetDateTime

@Service
class DomainEventPublisher(
  private val hmppsQueueService: HmppsQueueService,
  private val jsonMapper: JsonMapper,
  /**
   * Set as a message attribute which can then be used as filter in the
   * domain event consumer queue configuration. This is only used in
   * the dev and test environments which share the same underlying
   * domain event queue
   */
  @param:Value("\${community-payback.domain-events.environment:null}")
  private val environment: String?,
) {
  private val domainEventsTopic by lazy {
    hmppsQueueService.findByTopicId("hmppseventtopic") ?: error("hmppseventtopic not found")
  }

  fun publish(domainEvent: HmppsDomainEvent) {
    domainEventsTopic.publish(
      eventType = domainEvent.eventType,
      event = jsonMapper.writeValueAsString(domainEvent),
      attributes = environment?.let {
        mapOf("environment" to MessageAttributeValue.builder().dataType("String").stringValue(environment).build())
      } ?: emptyMap(),
    )
  }
}

// https://github.com/ministryofjustice/hmpps-domain-events/blob/main/schema/hmpps-domain-event.json
data class HmppsDomainEvent(
  val eventType: String,
  val version: Int,
  val description: String?,
  val detailUrl: String? = null,
  val occurredAt: OffsetDateTime = OffsetDateTime.now(),
  val additionalInformation: HmppsAdditionalInformation? = null,
  val personReference: HmmpsEventPersonReferences? = null,
)

data class HmppsAdditionalInformation(val map: Map<String, Any?> = mapOf())

data class HmmpsEventPersonReferences(
  val identifiers: List<HmmpsEventPersonReference>,
)

data class HmmpsEventPersonReference(
  val type: String,
  val value: String,
)
