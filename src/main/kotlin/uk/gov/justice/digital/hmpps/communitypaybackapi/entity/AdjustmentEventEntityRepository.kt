package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface AdjustmentEventEntityRepository : JpaRepository<AdjustmentEventEntity, UUID> {
  fun findByAppointmentOrderByCreatedAtAsc(appointment: AppointmentEntity): List<AdjustmentEventEntity>

  @Query(
    """
    select event from AdjustmentEventEntity event
    join fetch event.appointment
    where event.id = :id AND event.eventType = :eventType""",
  )
  fun findByIdOrNullForDomainEventDetails(id: UUID, eventType: AdjustmentEventType): AdjustmentEventEntity?

  @Modifying
  @Query("update AdjustmentEventEntity set triggeredSchedulingAt = :now, triggeredSchedulingId = :schedulingId where id = :eventId")
  fun setSchedulingRanAt(eventId: UUID, schedulingId: UUID, now: OffsetDateTime)
}
