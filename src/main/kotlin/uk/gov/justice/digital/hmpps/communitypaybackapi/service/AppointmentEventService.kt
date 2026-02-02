package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
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
) {
  fun getCreatedDomainEventDetails(id: UUID) = appointmentEventEntityRepository.findByIdOrNullForDomainEventDetails(id, AppointmentEventType.CREATE)?.toAppointmentCreatedDomainEvent()
  fun getUpdateDomainEventDetails(id: UUID) = appointmentEventEntityRepository.findByIdOrNullForDomainEventDetails(id, AppointmentEventType.UPDATE)?.toAppointmentUpdatedDomainEvent()

  fun getEvent(eventId: UUID): AppointmentEventEntity? = appointmentEventEntityRepository.findByIdOrNull(eventId)

  @Transactional
  fun recordSchedulingRan(
    forEventId: UUID,
    schedulingId: UUID,
  ) = appointmentEventEntityRepository.setSchedulingRanAt(
    eventId = forEventId,
    schedulingId = schedulingId,
    now = OffsetDateTime.now(),
  )
}
