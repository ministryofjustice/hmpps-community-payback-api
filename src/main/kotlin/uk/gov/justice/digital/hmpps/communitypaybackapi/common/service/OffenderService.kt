package uk.gov.justice.digital.hmpps.communitypaybackapi.common.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CaseAccess
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CommunityPaybackAndDeliusClient

@Service
class OffenderService(
  val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
  val contextService: ContextService,
) {

  companion object {
    const val MAX_OFFENDER_REQUEST_COUNT = 500
  }

  fun getOffenderInfo(crn: String) = getOffenderInfo(setOf(crn))[0]

  fun getOffenderInfo(
    crns: Set<String>,
  ): List<OffenderInfoResult> {
    require(crns.size <= MAX_OFFENDER_REQUEST_COUNT) { "Can only request up-to $MAX_OFFENDER_REQUEST_COUNT CRNs. Have requested ${crns.size}." }

    if (crns.isEmpty()) {
      return emptyList()
    }

    val caseSummaryByCrn = communityPaybackAndDeliusClient.getCaseSummaries(crns)
      .cases.associateBy(keySelector = { it.crn })

    val laoCrns = crns.filter { caseSummaryByCrn[it]?.hasLimitations() == true }.toSet()

    val userAccessByCrn = if (laoCrns.isNotEmpty()) {
      communityPaybackAndDeliusClient
        .getUsersAccess(contextService.getUserName(), laoCrns)
        .access.associateBy(keySelector = { it.crn })
    } else {
      emptyMap()
    }

    return crns.map { crn ->
      val caseSummary = caseSummaryByCrn[crn]
      val userAccess = userAccessByCrn[crn]

      when {
        caseSummary == null -> OffenderInfoResult.NotFound(crn)
        userAccess?.isLimited() == true -> OffenderInfoResult.Limited(crn)
        else -> OffenderInfoResult.Full(crn, caseSummary)
      }
    }
  }
}

private fun CaseSummary.hasLimitations() = this.currentExclusion || this.currentRestriction
private fun CaseAccess.isLimited() = this.userExcluded || this.userRestricted

sealed interface OffenderInfoResult {
  val crn: String

  data class Full(override val crn: String, val summary: CaseSummary) : OffenderInfoResult {
    companion object
  }
  data class Limited(override val crn: String) : OffenderInfoResult
  data class NotFound(override val crn: String) : OffenderInfoResult
}
