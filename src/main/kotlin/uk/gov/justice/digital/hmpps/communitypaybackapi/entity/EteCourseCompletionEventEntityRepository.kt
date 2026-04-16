package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface EteCourseCompletionEventEntityRepository : JpaRepository<EteCourseCompletionEventEntity, UUID> {

  @Query(
    """
    SELECT e FROM EteCourseCompletionEventEntity e
    LEFT JOIN e.resolution r
    WHERE e.status = :completionStatus
    AND e.pdu.providerCode = :providerCode 
    AND ((CAST(:pduId AS uuid) IS NULL) OR (e.pdu.id = :pduId))
    AND (:officesCount = 0 OR e.office IN :offices)
    AND ((:#{#resolutionStatus.name()} = 'ANY') OR (:#{#resolutionStatus.name()} = 'RESOLVED' AND r IS NOT NULL) OR (:#{#resolutionStatus.name()} = 'UNRESOLVED' AND r IS NULL))
    AND (:attempts IS NULL OR e.attempts = :attempts)
    AND (:externalReference IS NULL OR e.externalReference = :externalReference)
    AND (cast(:fromDate as timestamp) IS NULL OR e.completionDateTime >= :fromDate)
    AND (cast(:toDate as timestamp) IS NULL OR e.completionDateTime <= :toDate)
  """,
  )
  fun findAllWithFilters(
    providerCode: String,
    pduId: UUID?,
    officesCount: Int,
    offices: List<String>,
    resolutionStatus: ResolutionStatus,
    completionStatus: EteCourseCompletionEventStatus,
    attempts: Int?,
    externalReference: String?,
    fromDate: OffsetDateTime?,
    toDate: OffsetDateTime?,
    pageable: Pageable,
  ): Page<EteCourseCompletionEventEntity>

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

  enum class ResolutionStatus {
    ANY,
    RESOLVED,
    UNRESOLVED,
  }
}
