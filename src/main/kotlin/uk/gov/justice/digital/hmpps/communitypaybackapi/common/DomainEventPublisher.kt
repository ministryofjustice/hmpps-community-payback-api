package uk.gov.justice.digital.hmpps.communitypaybackapi.common

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.publish
import java.time.OffsetDateTime

@Service
class DomainEventPublisher(
  private val hmppsQueueService: HmppsQueueService,
  private val objectMapper: ObjectMapper,
) {
  private val domainEventsTopic by lazy {
    hmppsQueueService.findByTopicId("hmppseventtopic") ?: error("hmppseventtopic not found")
  }

  fun publish(domainEvent: HmppsDomainEvent) {
    domainEventsTopic.publish(
      eventType = domainEvent.eventType,
      event = objectMapper.writeValueAsString(domainEvent),
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

data class HmppsAdditionalInformation(val map: MutableMap<String, Any?> = mutableMapOf())

data class HmmpsEventPersonReferences(
  val identifiers: List<HmmpsEventPersonReference>,
)

data class HmmpsEventPersonReference(
  val type: String,
  val value: String,
)
