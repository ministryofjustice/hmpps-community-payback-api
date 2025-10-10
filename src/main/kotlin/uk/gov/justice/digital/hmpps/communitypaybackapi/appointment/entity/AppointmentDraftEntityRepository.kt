package uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AppointmentDraftEntityRepository : JpaRepository<AppointmentDraftEntity, UUID> {
  fun findByAppointmentDeliusId(appointmentDeliusId: Long): AppointmentDraftEntity?
}
