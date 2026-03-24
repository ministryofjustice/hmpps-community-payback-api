package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
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
  fun buildCreatedEvent(
    appointment: AppointmentEntity,
    trigger: AppointmentEventTrigger,
    validatedCreateAppointmentDto: Validated<CreateAppointmentDto>,
  ) = appointmentEventEntityFactory.buildCreatedEvent(appointment, trigger, validatedCreateAppointmentDto)

  fun buildUpdatedEvent(
    validatedUpdate: Validated<UpdateAppointmentOutcomeDto>,
    appointment: AppointmentEntity,
    existingAppointment: AppointmentDto,
    trigger: AppointmentEventTrigger,
    projectCode: String,
  ) = appointmentEventEntityFactory.buildUpdatedEvent(validatedUpdate, appointment, existingAppointment, trigger, projectCode)

  fun getCreatedDomainEventDetails(id: UUID) = appointmentEventEntityRepository.findByIdOrNullForDomainEventDetails(id, AppointmentEventType.CREATE)?.toAppointmentCreatedDomainEvent()

  fun getUpdateDomainEventDetails(id: UUID) = appointmentEventEntityRepository.findByIdOrNullForDomainEventDetails(id, AppointmentEventType.UPDATE)?.toAppointmentUpdatedDomainEvent()

  fun getEvent(eventId: UUID): AppointmentEventEntity? = appointmentEventEntityRepository.findByIdOrNull(eventId)

  fun hasUpdateAlreadyBeenSent(proposedUpdate: AppointmentEventEntity): Boolean {
    if (proposedUpdate.eventType != AppointmentEventType.UPDATE) {
      error("Can only check if an update has already been sent for events of type UPDATE")
    }

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

  @Transactional(Transactional.TxType.REQUIRED)
  fun saveAndThenPublishOnTransactionCommit(
    event: AppointmentEventEntity,
  ): AppointmentEventEntity = saveAndThenPublishOnTransactionCommit(listOf(event)).first()

  @Transactional(Transactional.TxType.REQUIRED)
  fun saveAndThenPublishOnTransactionCommit(
    events: List<AppointmentEventEntity>,
  ): List<AppointmentEventEntity> {
    val persistedEvents = appointmentEventEntityRepository.saveAll(events)

    persistedEvents.forEach { event ->
      domainEventService.publishOnTransactionCommit(
        id = event.id,
        type = when (event.eventType) {
          AppointmentEventType.CREATE -> DomainEventType.APPOINTMENT_CREATED
          AppointmentEventType.UPDATE -> DomainEventType.APPOINTMENT_UPDATED
        },
        additionalInformation = mapOf(
          AdditionalInformationType.APPOINTMENT_ID to event.appointment.id,
          AdditionalInformationType.DELIUS_APPOINTMENT_ID to event.appointment.deliusId,
        ),
        personReferences = mapOf(PersonReferenceType.CRN to event.appointment.crn),
      )
    }

    return persistedEvents
  }
}
