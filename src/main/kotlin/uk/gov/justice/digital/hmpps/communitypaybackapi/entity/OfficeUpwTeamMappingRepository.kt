package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface OfficeUpwTeamMappingRepository : JpaRepository<OfficeUpwTeamMappingEntity, UUID> {
  fun findByPduAndOffice(pdu: CommunityCampusPduEntity, office: String): OfficeUpwTeamMappingEntity?
}
