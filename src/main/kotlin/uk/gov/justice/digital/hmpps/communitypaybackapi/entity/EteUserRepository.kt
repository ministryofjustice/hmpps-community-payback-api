package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface EteUserRepository : JpaRepository<EteUser, UUID> {
  fun findByEmail(emailAddress: String): List<EteUser>
  fun findByCrn(crn: String): List<EteUser>

  @Query(
    """
    SELECT u FROM EteUser u 
    JOIN u.courseCompletions c 
    WHERE c.email = :email
  """,
  )
  fun findUsersWithCourseCompletionsByEmail(@Param("email") email: String): List<EteUser>

  @Query(
    """
    SELECT DISTINCT u FROM EteUser u 
    JOIN u.courseCompletions c 
    WHERE c.region = :region
  """,
  )
  fun findUsersByCourseRegion(@Param("region") region: String): List<EteUser>

  fun findByCrnAndEmail(crn: String, emailAddress: String): EteUser?
}
