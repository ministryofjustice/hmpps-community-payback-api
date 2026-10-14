package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.communitypaybackapi.config.CourseCompletionVisibilityConfig
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface EteCourseCompletionEventEntityRepository :
  JpaRepository<EteCourseCompletionEventEntity, UUID>,
  JpaSpecificationExecutor<EteCourseCompletionEventEntity> {

  fun findAllWithFilters(
    providerCode: String,
    pduId: UUID?,
    officesCount: Int,
    offices: List<String>,
    resolutionStatus: ResolutionStatus,
    courseFailures: CourseFailureFilter,
    externalReference: String?,
    fromDate: OffsetDateTime?,
    toDate: OffsetDateTime?,
    pageable: Pageable,
    visibility: CourseCompletionVisibilityConfig? = null,
  ): Page<EteCourseCompletionEventEntity> = findAll(
    Specification.allOf(
      EteCourseCompletionEventSpecifications.provider(providerCode),
      EteCourseCompletionEventSpecifications.pdu(pduId),
      EteCourseCompletionEventSpecifications.offices(offices.takeIf { officesCount > 0 }),
      EteCourseCompletionEventSpecifications.resolution(resolutionStatus),
      EteCourseCompletionEventSpecifications.courseFailures(courseFailures),
      EteCourseCompletionEventSpecifications.externalReference(externalReference),
      EteCourseCompletionEventSpecifications.completedFrom(fromDate),
      EteCourseCompletionEventSpecifications.completedTo(toDate),
      EteCourseCompletionEventSpecifications.visibleOffices(visibility),
    ),
    pageable,
  )

  @Query(
    """SELECT e FROM EteCourseCompletionEventEntity e
    WHERE e.resolution.crn = :crn AND 
    ((cast(:fromDateInclusive as timestamp) IS NULL) OR ((e.receivedAt >= :fromDateInclusive) OR (e.resolution.createdAt >= :fromDateInclusive))) AND 
    ((cast(:toDateTimeExclusive as timestamp) IS NULL) OR ((e.receivedAt < :toDateTimeExclusive) OR (e.resolution.createdAt < :toDateTimeExclusive)))
    ORDER BY e.createdAt DESC""",
  )
  fun findByCrnAndReceivedAt(
    crn: String,
    fromDateInclusive: OffsetDateTime?,
    toDateTimeExclusive: OffsetDateTime?,
  ): List<EteCourseCompletionEventEntity>

  @Query(
    """
    SELECT e FROM EteCourseCompletionEventEntity e
    WHERE e.pdu.providerCode = :providerCode 
    AND e.externalReference = :externalReference
    AND e.attempts BETWEEN :startAttempt AND :endAttempt
    ORDER BY e.attempts ASC
  """,
  )
  fun findBlock(
    providerCode: String,
    externalReference: String,
    startAttempt: Int,
    endAttempt: Int,
  ): List<EteCourseCompletionEventEntity>

  enum class ResolutionStatus {
    ANY,
    RESOLVED,
    UNRESOLVED,
  }

  enum class CourseFailureFilter {
    HIDE,
    SHOW_ALL,
    SHOW_ONLY_WHEN_MAX_ATTEMPTS_REACHED,
  }
}
