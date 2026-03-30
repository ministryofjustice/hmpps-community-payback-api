package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface AppointmentEntityRepository : JpaRepository<AppointmentEntity, UUID> {
  fun findByDeliusId(deliusId: Long): AppointmentEntity?

  @Query("select a from AppointmentEntity a where a.id in :ids ORDER BY a.date ASC")
  fun findAllByIdOrderByDateAsc(ids: Iterable<UUID>): List<AppointmentEntity>

  @Query(
    value = """
      SELECT appointment_id FROM (
        SELECT 
        a.id as appointment_id, 
        (
            select count(*) 
            from adjustment_events e 
            where e.appointment_id = a.id AND 
            ((cast(:fromDateInclusive as timestamp) IS NULL) OR (e.triggered_at >= :fromDateInclusive)) AND
            ((cast(:toDateTimeExclusive as timestamp) IS NULL) OR (e.triggered_at < :toDateTimeExclusive))
        ) AS adjustment_event_count,
        (
            select count(*) 
            from appointment_events e 
            where e.appointment_id = a.id AND 
            ((cast(:fromDateInclusive as timestamp) IS NULL) OR (e.triggered_at >= :fromDateInclusive)) AND
            ((cast(:toDateTimeExclusive as timestamp) IS NULL) OR (e.triggered_at < :toDateTimeExclusive))
        ) AS appointment_event_count
        FROM appointments a WHERE crn = :crn
        GROUP BY a.id
      ) WHERE adjustment_event_count > 0 OR appointment_event_count > 0
    """,
    nativeQuery = true,
  )
  fun findAppointmentIdsWithEventsInRange(crn: String, fromDateInclusive: OffsetDateTime?, toDateTimeExclusive: OffsetDateTime?): List<UUID>
}
