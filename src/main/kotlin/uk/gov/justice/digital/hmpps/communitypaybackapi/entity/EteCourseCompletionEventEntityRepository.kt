package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository
interface EteCourseCompletionEventEntityRepository : JpaRepository<EteCourseCompletionEventEntity, UUID> {

  @Query(
    """
    SELECT e FROM EteCourseCompletionEventEntity e 
    WHERE e.region = :region 
    AND (:fromDate IS NULL OR e.completionDate >= :fromDate)
    AND (:toDate IS NULL OR e.completionDate <= :toDate)
  """,
  )
  fun findByRegionAndDateRange(
    @Param("region") region: String,
    @Param("fromDate") fromDate: LocalDate?,
    @Param("toDate") toDate: LocalDate?,
    pageable: Pageable,
  ): Page<EteCourseCompletionEventEntity>

  @Query("SELECT e FROM EteCourseCompletionEventEntity e WHERE e.user.crn = :crn")
  fun findByCrn(@Param("crn") crn: String): List<EteCourseCompletionEventEntity>

  fun findByEmail(@Param("email") email: String): List<EteCourseCompletionEventEntity>
}
