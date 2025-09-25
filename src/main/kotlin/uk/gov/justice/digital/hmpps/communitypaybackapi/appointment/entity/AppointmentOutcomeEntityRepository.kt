package uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AppointmentOutcomeEntityRepository : JpaRepository<AppointmentOutcomeEntity, UUID> {

  @Query(
    """
    select ao from AppointmentOutcomeEntity ao
    join fetch ao.contactOutcomeEntity 
    join fetch ao.enforcementActionEntity
    join fetch ao.projectTypeEntity
    where ao.id = :id""",
  )
  fun findByIdOrNullForDomainEventDetails(id: UUID): AppointmentOutcomeEntity?

  fun findTopByAppointmentDeliusIdOrderByUpdatedAtDesc(appointmentDeliusId: Long): AppointmentOutcomeEntity?
}
