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
    WHERE e.region = :region 
    AND (:officesCount = 0 OR e.office IN :offices)
    AND (cast(:fromDate as date) IS NULL OR e.completionDate >= :fromDate)
    AND (cast(:toDate as date) IS NULL OR e.completionDate <= :toDate)
  """,
  )
  fun findAllWithFilters(
    region: String,
    officesCount: Int,
    offices: List<String>,
    fromDate: LocalDate?,
    toDate: LocalDate?,
    pageable: Pageable,
  ): Page<EteCourseCompletionEventEntity>
}
