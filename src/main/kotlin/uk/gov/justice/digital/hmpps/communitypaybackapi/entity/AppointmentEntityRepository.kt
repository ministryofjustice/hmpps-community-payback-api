package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AppointmentEntityRepository : JpaRepository<AppointmentEntity, UUID> {
  fun findByDeliusId(deliusId: Long): AppointmentEntity?
}
