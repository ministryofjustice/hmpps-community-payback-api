package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SessionSupervisorEntityRepository : JpaRepository<SessionSupervisorEntity, SessionSupervisorId>
