package uk.gov.justice.digital.hmpps.communitypaybackapi.common.service

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import org.springframework.transaction.event.TransactionalEventListener
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.internal.DomainEventPublisher
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.internal.HmppsAdditionalInformation
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.internal.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.internal.UrlTemplate
import java.time.OffsetDateTime
import java.util.UUID

@Service
open class DomainEventService(
  private val applicationEventPublisher: ApplicationEventPublisher,
  private val domainEventUrlConfig: DomainEventUrlConfig,
  private val domainEventPublisher: DomainEventPublisher,
) {

  fun publish(
    id: UUID,
    type: DomainEventType,
    additionalInformation: Map<AdditionalInformationType, Any> = emptyMap(),
  ) {
    applicationEventPublisher.publishEvent(
      PublishDomainEventCommand(
        HmppsDomainEvent(
          eventType = type.eventType,
          version = 1,
          description = type.description,
          detailUrl = resolveUrl(id, type),
          occurredAt = OffsetDateTime.now(),
          additionalInformation = additionalInformation.toHmppsAdditionalInformation(),
        ),
      ),
    )
  }

  @TransactionalEventListener(fallbackExecution = true)
  fun publishDomainEventCommandListener(command: PublishDomainEventCommand) {
    domainEventPublisher.publish(command.domainEvent)
  }

  private fun resolveUrl(id: UUID, type: DomainEventType): String {
    val key = type.name.lowercase()
    val urlTemplate = domainEventUrlConfig.domainEventDetail[key] ?: error("Could not find domain event url with key '$key'")
    return urlTemplate.resolve(mapOf("id" to id.toString()))
  }

  private fun Map<AdditionalInformationType, Any>.toHmppsAdditionalInformation() = if (this.isEmpty()) {
    null
  } else {
    HmppsAdditionalInformation(mapKeys { it.key.name })
  }

  data class PublishDomainEventCommand(val domainEvent: HmppsDomainEvent) : ApplicationEvent(domainEvent)
}

enum class DomainEventType(
  val eventType: String,
  val description: String,
) {
  APPOINTMENT_OUTCOME(
    eventType = "community-payback.appointment.outcome",
    description = "A community payback appointment has been updated with an outcome",
  ),
}

enum class AdditionalInformationType {
  APPOINTMENT_ID,
}

@Configuration
@ConfigurationProperties(prefix = "community-payback.url-templates")
class DomainEventUrlConfig {
  lateinit var domainEventDetail: Map<String, UrlTemplate>
}
