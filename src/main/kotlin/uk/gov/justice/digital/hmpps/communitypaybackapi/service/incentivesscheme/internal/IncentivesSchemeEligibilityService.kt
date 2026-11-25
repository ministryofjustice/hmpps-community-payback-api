package uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivesscheme.internal

import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.IncentivesSchemeEligibilityScope
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.IncentivesSchemeEligibilityScopeEntityRepository

@Service
class IncentivesSchemeEligibilityService(
  private val incentivesSchemeEligibilityScopeEntityRepository: IncentivesSchemeEligibilityScopeEntityRepository,
  private val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
) {
  fun isEligible(crn: String, deliusEventNumber: Int): Boolean {
    val caseSummary = communityPaybackAndDeliusClient.getCaseSummary(crn) ?: return false

    val upwDetails = caseSummary.unpaidWorkDetails.firstOrNull { it.eventNumber == deliusEventNumber } ?: return false

    if (!incentivesSchemeEligibilityScopeEntityRepository.isEligible(IncentivesSchemeEligibilityScope.OUTCOME, upwDetails.eventOutcomeCode)) return false

    return upwDetails.unpaidWorkRequirements.any {
      it.subType != null &&
        incentivesSchemeEligibilityScopeEntityRepository.isEligible(
          IncentivesSchemeEligibilityScope.REQUIREMENT_SUBTYPE,
          it.subType.code,
        )
    }
  }

  private fun CommunityPaybackAndDeliusClient.getCaseSummary(crn: String) = try {
    this.getUpwDetailsSummary(crn, null)
  } catch (_: WebClientResponseException.NotFound) {
    null
  }
}
