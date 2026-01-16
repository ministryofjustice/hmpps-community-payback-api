package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface AppointmentOutcomeEntityRepository : JpaRepository<AppointmentOutcomeEntity, UUID> {

  @Query(
    """
    select ao from AppointmentOutcomeEntity ao
    left join fetch ao.contactOutcome
    where ao.id = :id""",
  )
  fun findByIdOrNullForDomainEventDetails(id: UUID): AppointmentOutcomeEntity?

  fun findTopByAppointmentDeliusIdOrderByCreatedAtDesc(appointmentDeliusId: Long): AppointmentOutcomeEntity?

  @Modifying
  @Query("update AppointmentOutcomeEntity set schedulingRanAt = :now where id = :updateId")
  fun setSchedulingRanAt(updateId: UUID, now: OffsetDateTime)
}
