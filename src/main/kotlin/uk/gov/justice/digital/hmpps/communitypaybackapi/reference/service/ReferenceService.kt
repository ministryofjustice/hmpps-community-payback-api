package uk.gov.justice.digital.hmpps.communitypaybackapi.reference.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CommunityPaybackAndDeliusClient

@Service
class ReferenceService(
  val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
) {
  fun getProjectTypes() = communityPaybackAndDeliusClient.getProjectTypes().toDto()
}
