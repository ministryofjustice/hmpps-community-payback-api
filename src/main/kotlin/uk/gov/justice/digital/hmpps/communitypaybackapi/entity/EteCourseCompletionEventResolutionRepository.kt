package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface EteCourseCompletionEventResolutionRepository : JpaRepository<EteCourseCompletionEventResolutionEntity, UUID> {
  fun findAllByEteCourseCompletionEventEmailOrderByCreatedAtDesc(email: String): List<EteCourseCompletionEventResolutionEntity>

  fun findAllByEteCourseCompletionEventOfficeAndEteCourseCompletionEventCourseNameOrderByCreatedAtDesc(
    office: String,
    courseName: String,
  ): List<EteCourseCompletionEventResolutionEntity>
}
