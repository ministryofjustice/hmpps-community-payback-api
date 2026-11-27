package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface IncentivesSchemeEligibilityScopeEntityRepository : JpaRepository<IncentivesSchemeEligibilityScopeEntity, UUID> {
  @Query(
    """
      SELECT COUNT(*) > 0
      FROM IncentivesSchemeEligibilityScopeEntity es
      WHERE es.scope = :scope
      AND es.code = :code
      AND es.isEligible = true
    """,
  )
  fun isEligible(scope: IncentivesSchemeEligibilityScope, code: String): Boolean
}
