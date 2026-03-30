package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface AppointmentEventEntityRepository : JpaRepository<AppointmentEventEntity, UUID> {
  @Query(
    """
    select event from AppointmentEventEntity event
    join fetch event.appointment
    left join fetch event.contactOutcome
    where event.id = :id AND event.eventType = :eventType""",
  )
  fun findByIdOrNullForDomainEventDetails(id: UUID, eventType: AppointmentEventType): AppointmentEventEntity?

  fun findTopByAppointmentIdOrderByCreatedAtDesc(appointmentId: UUID): AppointmentEventEntity?

  fun findByTriggerTypeAndTriggeredBy(triggerType: AppointmentEventTriggerType, triggeredBy: String): AppointmentEventEntity?

  @Modifying
  @Query("update AppointmentEventEntity set triggeredSchedulingAt = :now, triggeredSchedulingId = :schedulingId where id = :eventId")
  fun setSchedulingRanAt(eventId: UUID, schedulingId: UUID, now: OffsetDateTime)

  fun findByAppointmentOrderByCreatedAtAsc(appointment: AppointmentEntity): List<AppointmentEventEntity>
}
