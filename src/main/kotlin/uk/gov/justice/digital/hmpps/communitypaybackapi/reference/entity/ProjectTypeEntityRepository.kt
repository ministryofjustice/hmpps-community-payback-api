package uk.gov.justice.digital.hmpps.communitypaybackapi.reference.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ProjectTypeEntityRepository : JpaRepository<ProjectTypeEntity, UUID>
