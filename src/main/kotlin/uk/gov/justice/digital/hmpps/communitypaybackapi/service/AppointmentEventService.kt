package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventType
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

  fun createUpdateEvent(
    validatedUpdate: UpdateAppointmentOutcomeDto,
    trigger: AppointmentEventTrigger,
    existingAppointment: AppointmentDto,
  ): EventCreationOutcome {
    val proposedEntity = appointmentEventEntityFactory.buildUpdatedEvent(validatedUpdate, existingAppointment, trigger)

    if (hasUpdateAlreadyBeenSent(proposedEntity)) {
      return EventCreationOutcome.DuplicateIgnored
    }

    val persistedEntity = appointmentEventEntityRepository.save(proposedEntity)

    domainEventService.publishOnTransactionCommit(
      id = persistedEntity.id,
      type = DomainEventType.APPOINTMENT_UPDATED,
      additionalInformation = mapOf(AdditionalInformationType.APPOINTMENT_ID to validatedUpdate.deliusId),
      personReferences = mapOf(PersonReferenceType.CRN to existingAppointment.offender.crn),
    )

    return EventCreationOutcome.EventCreated(
      persistedEntity,
    )
  }

  private fun hasUpdateAlreadyBeenSent(proposedEntity: AppointmentEventEntity) = appointmentEventEntityRepository
    .findTopByDeliusAppointmentIdOrderByCreatedAtDesc(proposedEntity.deliusAppointmentId)
    ?.isLogicallyIdentical(proposedEntity)
    ?: false

  @Transactional
  fun recordSchedulingRan(
    forEventId: UUID,
    schedulingId: UUID,
  ) = appointmentEventEntityRepository.setSchedulingRanAt(
    eventId = forEventId,
    schedulingId = schedulingId,
    now = OffsetDateTime.now(),
  )

  sealed interface EventCreationOutcome {
    object DuplicateIgnored : EventCreationOutcome
    data class EventCreated(val event: AppointmentEventEntity) : EventCreationOutcome
  }
}
