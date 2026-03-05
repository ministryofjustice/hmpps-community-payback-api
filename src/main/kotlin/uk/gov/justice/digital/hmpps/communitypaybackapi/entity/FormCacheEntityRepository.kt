package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

@Repository
interface FormCacheEntityRepository : JpaRepository<FormCacheEntity, FormCacheId> {
  @Modifying
  @Query("DELETE FROM FormCacheEntity WHERE updatedAt < :threshold ")
  fun deleteByLastUpdatedAtBefore(threshold: OffsetDateTime): Long
}
