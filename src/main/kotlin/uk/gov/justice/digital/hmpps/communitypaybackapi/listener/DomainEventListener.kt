package uk.gov.justice.digital.hmpps.communitypaybackapi.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdditionalInformationType
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentUpdateService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.DomainEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingService
import java.util.UUID

/**
 * The type of domain events this listener receives are managed by the cloud platform configuration
 * See https://tech-docs.hmpps.service.justice.gov.uk/common-kotlin-patterns/domain-events/
 */
@Service
@ConditionalOnProperty(name = ["community-payback.scheduling.enabled"], havingValue = "true")
class DomainEventListener(
  private val objectMapper: ObjectMapper,
  private val scheduleService: SchedulingService,
  private val appointmentUpdateService: AppointmentUpdateService,
) {
  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @SqsListener("hmppsdomaineventsqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun domainEvent(messageString: String) {
    log.debug("Have received domain event message '$messageString'")

    val sqsMessage = objectMapper.readValue<SqsMessage>(messageString)
    val event = objectMapper.readValue<HmppsDomainEvent>(sqsMessage.message)

    when (event.eventType) {
      DomainEventType.APPOINTMENT_UPDATED.eventType -> handleAppointmentUpdated(event)
      else -> log.warn("Unexpected event type ${event.eventType}")
    }
  }

  private fun handleAppointmentUpdated(event: HmppsDomainEvent) {
    val eventId = UUID.fromString(event.additionalInformation!!.map[AdditionalInformationType.EVENT_ID.name]!!.toString())
    val domainEventDetails = appointmentUpdateService.getAppointmentUpdatedDomainEventDetails(eventId)
      ?: error("Can't find appointment updated record for event id $eventId")

    scheduleService.scheduleAppointments(
      crn = domainEventDetails.crn,
      eventNumber = domainEventDetails.deliusEventNumber,
      trigger = "Appointment Updated",
    )

    appointmentUpdateService.recordSchedulingRan(eventId)
  }
}
