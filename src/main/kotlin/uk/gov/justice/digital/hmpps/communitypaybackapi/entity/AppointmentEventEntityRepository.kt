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
"""SELECT event FROM AppointmentEventEntity event
    LEFT JOIN FETCH event.contactOutcome
    WHERE event.crn = :crn AND 
    ((cast(:fromDateInclusive as timestamp) IS NULL) OR (event.triggeredAt >= :fromDateInclusive)) AND 
    ((cast(:toDateTimeExclusive as timestamp) IS NULL) OR (event.triggeredAt < :toDateTimeExclusive))
    ORDER BY event.createdAt DESC""",
  )
  fun findByCrnAndTriggeredAt(crn: String, fromDateInclusive: OffsetDateTime?, toDateTimeExclusive: OffsetDateTime?): List<AppointmentEventEntity>

  @Query(
    """
    select event from AppointmentEventEntity event
    left join fetch event.contactOutcome
    where event.id = :id AND event.eventType = :eventType""",
  )
  fun findByIdOrNullForDomainEventDetails(id: UUID, eventType: AppointmentEventType): AppointmentEventEntity?

  fun findTopByDeliusAppointmentIdOrderByCreatedAtDesc(deliusAppointmentId: Long): AppointmentEventEntity?

  @Modifying
  @Query("update AppointmentEventEntity set triggeredSchedulingAt = :now, triggeredSchedulingId = :schedulingId where id = :eventId")
  fun setSchedulingRanAt(eventId: UUID, schedulingId: UUID, now: OffsetDateTime)
}
