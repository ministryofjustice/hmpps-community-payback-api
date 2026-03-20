package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface CommunityCampusPduEntityRepository : JpaRepository<CommunityCampusPduEntity, UUID> {
  fun findAllByOrderByNameAsc(): List<CommunityCampusPduEntity>
  fun findByName(name: String): CommunityCampusPduEntity?
  fun findByNameIgnoreCase(name: String): CommunityCampusPduEntity?
}
