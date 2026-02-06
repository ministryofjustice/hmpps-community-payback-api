package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface EteUserRepository : JpaRepository<EteUser, UUID> {
  fun findByEmail(emailAddress: String): List<EteUser>
  fun findByCrn(crn: String): List<EteUser>
  fun findByCrnAndEmail(crn: String, lowercase: String): EteUser?
}
