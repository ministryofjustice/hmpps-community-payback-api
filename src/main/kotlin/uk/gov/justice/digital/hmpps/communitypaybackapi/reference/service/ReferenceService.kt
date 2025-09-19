package uk.gov.justice.digital.hmpps.communitypaybackapi.reference.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.entity.ContactOutcomeEntityRepository

@Service
class ReferenceService(
  val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
  val contactOutcomeEntityRepository: ContactOutcomeEntityRepository,
) {
  fun getProjectTypes() = communityPaybackAndDeliusClient.getProjectTypes().toDto()

  fun getContactOutcomes() = contactOutcomeEntityRepository.findAll().toDto()

  fun getEnforcementActions() = communityPaybackAndDeliusClient.getEnforcementActions().toDto()
}
