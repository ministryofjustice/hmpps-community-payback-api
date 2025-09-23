package uk.gov.justice.digital.hmpps.communitypaybackapi.reference.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.entity.EnforcementActionEntityRepository

@Service
class ReferenceService(
  val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
  val contactOutcomeEntityRepository: ContactOutcomeEntityRepository,
  val enforcementActionEntityRepository: EnforcementActionEntityRepository,
) {
  fun getProjectTypes() = communityPaybackAndDeliusClient.getProjectTypes().toDto()

  fun getContactOutcomes() = contactOutcomeEntityRepository.findAll().sortedBy { it.name }.toDto()

  fun getEnforcementActions() = enforcementActionEntityRepository.findAll().sortedBy { it.name }.toDto()
}
