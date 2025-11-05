package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface SessionSupervisorEntityRepository : JpaRepository<SessionSupervisorEntity, SessionSupervisorId> {
  fun findBySupervisorCodeAndDayGreaterThanEqualOrderByDayAsc(
    supervisorCode: String,
    minimumDate: LocalDate,
  ): List<SessionSupervisorEntity>
}
