package uk.gov.justice.digital.hmpps.communitypaybackapi.project.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CommunityPaybackAndDeliusClient
import java.time.LocalDate

@Service
class ProjectService(
  val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
) {
  fun getProjectAllocations(
    startDate: LocalDate,
    endDate: LocalDate,
    teamId: Long,
  ) = communityPaybackAndDeliusClient.getProjectAllocations(startDate, endDate, teamId).toDto()
}
