package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
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
}
