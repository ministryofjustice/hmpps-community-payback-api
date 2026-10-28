package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository
interface SessionSupervisorEntityRepository : JpaRepository<SessionSupervisorEntity, UUID> {
  fun findByProjectCodeAndDayAndSupervisorCode(projectCode: String, day: LocalDate?, supervisorCode: String): SessionSupervisorEntity?

  @Query("select s from SessionSupervisorEntity s where s.supervisorCode = :supervisorCode and (s.day is null or s.day >= :minimumDate) order by s.day asc")
  fun findAssignments(supervisorCode: String, minimumDate: LocalDate): List<SessionSupervisorEntity>
}
