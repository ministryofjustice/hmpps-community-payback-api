package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.AppointmentCreatedEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.UpdateAppointmentEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toAppointmentCreatedDomainEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toAppointmentUpdatedDomainEvent
import java.time.OffsetDateTime
import java.util.UUID

@Service
class AppointmentEventService(
  private val appointmentEventEntityRepository: AppointmentEventEntityRepository,
  private val appointmentEventEntityFactory: AppointmentEventEntityFactory,
  private val domainEventService: DomainEventService,
) {
  fun getCreatedDomainEventDetails(id: UUID) = appointmentEventEntityRepository.findByIdOrNullForDomainEventDetails(id, AppointmentEventType.CREATE)?.toAppointmentCreatedDomainEvent()

  fun getUpdateDomainEventDetails(id: UUID) = appointmentEventEntityRepository.findByIdOrNullForDomainEventDetails(id, AppointmentEventType.UPDATE)?.toAppointmentUpdatedDomainEvent()

  fun getEvent(eventId: UUID) = appointmentEventEntityRepository.findByIdOrNull(eventId)

  fun hasUpdateAlreadyBeenSent(proposedUpdateDetails: UpdateAppointmentEvent): Boolean {
    val proposedUpdate = appointmentEventEntityFactory.buildUpdatedEvent(proposedUpdateDetails)

    return appointmentEventEntityRepository
      .findTopByAppointmentIdOrderByCreatedAtDesc(proposedUpdate.appointment.id)
      ?.isLogicallyIdentical(proposedUpdate)
      ?: false
  }

  @Transactional
  fun recordSchedulingRan(
    forEventId: UUID,
    schedulingId: UUID,
  ) = appointmentEventEntityRepository.setSchedulingRanAt(
    eventId = forEventId,
    schedulingId = schedulingId,
    now = OffsetDateTime.now(),
  )

  fun persistAndPublishAppointmentCreatedDomainEvents(
    event: List<AppointmentCreatedEvent>,
  ) {
    persistAndPublishDomainEventsOnTransactionCommit(
      event.map {
        appointmentEventEntityFactory.buildCreatedEvent(it)
      },
    )
  }

  fun persistAndPublishAppointmentUpdateDomainEvent(
    event: UpdateAppointmentEvent,
  ) {
    persistAndPublishDomainEventsOnTransactionCommit(
      listOf(appointmentEventEntityFactory.buildUpdatedEvent(event)),
    )
  }

  private fun persistAndPublishDomainEventsOnTransactionCommit(events: List<AppointmentEventEntity>) {
    val persistedEvents = appointmentEventEntityRepository.saveAll(events)

    persistedEvents.forEach { event ->
      domainEventService.publishOnTransactionCommit(
        id = event.id,
        type = when (event.eventType) {
          AppointmentEventType.CREATE -> DomainEventType.APPOINTMENT_CREATED
          AppointmentEventType.UPDATE -> DomainEventType.APPOINTMENT_UPDATED
        },
        headers = event.appointment.toDomainEventHeaders(),
      )
    }
  }
}
