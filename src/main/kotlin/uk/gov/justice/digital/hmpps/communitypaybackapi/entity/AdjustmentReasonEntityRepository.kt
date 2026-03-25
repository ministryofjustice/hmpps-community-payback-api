package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AdjustmentReasonEntityRepository : JpaRepository<AdjustmentReasonEntity, UUID> {
  fun findAllByOrderByNameAsc(): List<AdjustmentReasonEntity>
}
