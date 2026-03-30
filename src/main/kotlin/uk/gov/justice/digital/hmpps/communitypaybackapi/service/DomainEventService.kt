package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.event.TransactionalEventListener
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
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
  @param:Value("\${community-payback.domain-events.details-url-pattern}")
  private val detailUrlPattern: UrlTemplate,
  private val domainEventPublisher: DomainEventPublisher,
) {

  data class EventHeaders(
    val additionalInformation: Map<AdditionalInformationType, Any> = emptyMap(),
    val personReferences: Map<PersonReferenceType, String> = emptyMap(),
  )

  @Transactional(Transactional.TxType.REQUIRED)
  fun publishOnTransactionCommit(
    id: UUID,
    type: DomainEventType,
    headers: EventHeaders,
  ) {
    applicationEventPublisher.publishEvent(
      PublishDomainEventCommand(
        HmppsDomainEvent(
          eventType = type.eventType,
          version = 1,
          description = type.description,
          detailUrl = resolveUrl(id, type),
          occurredAt = OffsetDateTime.now(),
          additionalInformation = buildAdditionalInformation(id, headers.additionalInformation),
          personReference = headers.personReferences.toHmppsPersonReference(),
        ),
      ),
    )
  }

  @TransactionalEventListener(fallbackExecution = true)
  fun publishDomainEventCommandListener(command: PublishDomainEventCommand) {
    domainEventPublisher.publish(command.domainEvent)
  }

  private fun resolveUrl(id: UUID, type: DomainEventType) = detailUrlPattern.resolve(
    mapOf(
      "type" to type.urlType,
      "id" to id.toString(),
    ),
  )

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
  val urlType: String,
  val description: String,
) {
  ADJUSTMENT_CREATED(
    eventType = "community-payback.adjustment.created",
    urlType = "adjustment-created",
    description = "A community payback adjustment has been created",
  ),
  APPOINTMENT_CREATED(
    eventType = "community-payback.appointment.created",
    urlType = "appointment-created",
    description = "A community payback appointment has been created",
  ),
  APPOINTMENT_UPDATED(
    eventType = "community-payback.appointment.updated",
    urlType = "appointment-updated",
    description = "A community payback appointment has been updated",
  ),
}

enum class AdditionalInformationType {
  APPOINTMENT_ID,
  DELIUS_APPOINTMENT_ID,
  EVENT_ID,
}

enum class PersonReferenceType {
  CRN,
}

fun AppointmentEntity.toDomainEventHeaders() = DomainEventService.EventHeaders(
  additionalInformation = mapOf(
    AdditionalInformationType.APPOINTMENT_ID to id,
    AdditionalInformationType.DELIUS_APPOINTMENT_ID to deliusId,
  ),
  personReferences = mapOf(PersonReferenceType.CRN to crn),
)
