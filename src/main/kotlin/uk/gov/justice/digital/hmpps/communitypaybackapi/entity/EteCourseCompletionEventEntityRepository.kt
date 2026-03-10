package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository
interface EteCourseCompletionEventEntityRepository : JpaRepository<EteCourseCompletionEventEntity, UUID> {

  @Query(
    """
    SELECT e FROM EteCourseCompletionEventEntity e
    LEFT JOIN e.resolution r
    WHERE e.status = 'PASSED' 
    AND e.pdu.providerCode = :providerCode 
    AND ((CAST(:pduId AS uuid) IS NULL) OR (e.pdu.id = :pduId))
    AND (:officesCount = 0 OR e.office IN :offices)
    AND ((:#{#resolutionStatus.name()} = 'ANY') OR (:#{#resolutionStatus.name()} = 'RESOLVED' AND r IS NOT NULL) OR (:#{#resolutionStatus.name()} = 'UNRESOLVED' AND r IS NULL))
    AND (cast(:fromDate as date) IS NULL OR e.completionDate >= :fromDate)
    AND (cast(:toDate as date) IS NULL OR e.completionDate <= :toDate)
  """,
  )
  fun findAllPassedWithFilters(
    providerCode: String,
    pduId: UUID?,
    officesCount: Int,
    offices: List<String>,
    resolutionStatus: ResolutionStatus,
    fromDate: LocalDate?,
    toDate: LocalDate?,
    pageable: Pageable,
  ): Page<EteCourseCompletionEventEntity>

  enum class ResolutionStatus {
    ANY,
    RESOLVED,
    UNRESOLVED,
  }
}
