package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository
interface AppointmentTaskEntityRepository : JpaRepository<AppointmentTaskEntity, UUID> {
  fun findByAppointmentId(appointmentId: UUID): List<AppointmentTaskEntity>

  @Query(
    """
    SELECT task FROM AppointmentTaskEntity task
    INNER JOIN AppointmentEntity appointment ON task.appointmentId = appointment.id
    WHERE task.taskStatus = 'PENDING'
    AND ((cast(:fromDate as date) IS NULL) OR (appointment.date >= :fromDate))
    AND ((cast(:toDate as date) IS NULL) OR (appointment.date <= :toDate))
    AND ((cast(:providerCode as string) IS NULL) OR (appointment.providerCode = :providerCode))
    ORDER BY task.createdAt DESC
    """,
  )
  fun findPendingTasksWithFilters(
    fromDate: LocalDate?,
    toDate: LocalDate?,
    providerCode: String?,
    pageable: Pageable,
  ): Page<AppointmentTaskEntity>
}
