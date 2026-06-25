package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface EteCourseCompletionDraftResolutionRepository : JpaRepository<EteCourseCompletionDraftResolutionEntity, UUID> {
  fun findByEteCourseCompletionEventId(id: UUID): EteCourseCompletionDraftResolutionEntity?
}
