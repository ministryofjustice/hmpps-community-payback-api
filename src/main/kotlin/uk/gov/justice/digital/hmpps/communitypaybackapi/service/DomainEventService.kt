package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import org.springframework.transaction.event.TransactionalEventListener
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.DomainEventPublisher
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.HmmpsEventPersonReference
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.HmmpsEventPersonReferences
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.HmppsAdditionalInformation
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.UrlTemplate
import java.time.OffsetDateTime
import java.util.UUID

@Service
class DomainEventService(
  private val applicationEventPublisher: ApplicationEventPublisher,
  private val domainEventUrlConfig: DomainEventUrlConfig,
  private val domainEventPublisher: DomainEventPublisher,
) {

  init {
    ensureUrlConfigExists()
  }

  private fun ensureUrlConfigExists() {
    DomainEventType.entries.forEach {
      resolveUrl(UUID.randomUUID(), it)
    }
  }

  fun publishOnTransactionCommit(
    id: UUID,
    type: DomainEventType,
    additionalInformation: Map<AdditionalInformationType, Any> = emptyMap(),
    personReferences: Map<PersonReferenceType, String> = emptyMap(),
  ) {
    applicationEventPublisher.publishEvent(
      PublishDomainEventCommand(
        HmppsDomainEvent(
          eventType = type.eventType,
          version = 1,
          description = type.description,
          detailUrl = resolveUrl(id, type),
          occurredAt = OffsetDateTime.now(),
          additionalInformation = buildAdditionalInformation(id, additionalInformation),
          personReference = personReferences.toHmppsPersonReference(),
        ),
      ),
    )
  }

  @TransactionalEventListener(fallbackExecution = true)
  fun publishDomainEventCommandListener(command: PublishDomainEventCommand) {
    domainEventPublisher.publish(command.domainEvent)
  }

  private fun resolveUrl(id: UUID, type: DomainEventType): String {
    val urlTemplate = when (type) {
      DomainEventType.APPOINTMENT_UPDATED -> domainEventUrlConfig.appointmentUpdated
    }
    return urlTemplate.resolve(mapOf("id" to id.toString()))
  }

  private fun buildAdditionalInformation(
    id: UUID,
    additionalInformation: Map<AdditionalInformationType, Any>,
  ): HmppsAdditionalInformation = HmppsAdditionalInformation(
    map = mapOf(AdditionalInformationType.EVENT_ID.name to id).plus(additionalInformation.mapKeys { it.key.name }),
  )

  private fun Map<PersonReferenceType, String>.toHmppsPersonReference() = if (this.isEmpty()) {
    null
  } else {
    HmmpsEventPersonReferences(map { HmmpsEventPersonReference(it.key.name, it.value) })
  }

  data class PublishDomainEventCommand(val domainEvent: HmppsDomainEvent) : ApplicationEvent(domainEvent)
}

enum class DomainEventType(
  val eventType: String,
  val description: String,
) {
  APPOINTMENT_UPDATED(
    eventType = "community-payback.appointment.updated",
    description = "A community payback appointment has been updated",
  ),
}

enum class AdditionalInformationType {
  APPOINTMENT_ID,
  EVENT_ID,
}

enum class PersonReferenceType {
  CRN,
}

@Configuration
@ConfigurationProperties(prefix = "community-payback.domain-events.urls")
class DomainEventUrlConfig {
  lateinit var appointmentUpdated: UrlTemplate
}
