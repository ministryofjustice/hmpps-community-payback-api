package uk.gov.justice.digital.hmpps.communitypaybackapi.project.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AppointmentOutcomeEntityRepository : JpaRepository<AppointmentOutcomeEntity, UUID>
