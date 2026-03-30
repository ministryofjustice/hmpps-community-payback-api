package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentEventEntityFactory.CreateAppointmentEventDetails
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentEventEntityFactory.UpdateAppointmentEventDetails
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

  fun getEvent(eventId: UUID): AppointmentEventEntity? = appointmentEventEntityRepository.findByIdOrNull(eventId)

  fun hasUpdateAlreadyBeenSent(proposedUpdateDetails: UpdateAppointmentEventDetails): Boolean {
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

  fun publishUpdateEventOnTransactionCommit(
    eventDetails: UpdateAppointmentEventDetails,
  ) {
    saveAndPublishEventsOnTransactionCommit(
      listOf(appointmentEventEntityFactory.buildUpdatedEvent(eventDetails)),
    )
  }

  fun publishCreateEventsOnTransactionCommit(
    eventDetails: List<CreateAppointmentEventDetails>,
  ) {
    saveAndPublishEventsOnTransactionCommit(
      eventDetails.map {
        appointmentEventEntityFactory.buildCreatedEvent(it)
      },
    )
  }

  private fun saveAndPublishEventsOnTransactionCommit(events: List<AppointmentEventEntity>) {
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
