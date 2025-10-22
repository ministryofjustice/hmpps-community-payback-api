package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ArnsClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CaseAccess
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.NotFoundException

@Service
class OffenderService(
  val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
  val arnsClient: ArnsClient,
  val contextService: ContextService,
) {

  fun toOffenderInfo(
    caseSummary: CaseSummary,
  ) = toOffenderInfos(listOf(caseSummary))[0]

  fun toOffenderInfos(
    caseSummaries: List<CaseSummary>,
  ): List<OffenderInfoResult> {
    val laoCrns = caseSummaries.filter { it.hasLimitations() }.map { it.crn }.toSet()

    val userAccessByCrn = if (laoCrns.isNotEmpty()) {
      communityPaybackAndDeliusClient
        .getUsersAccess(contextService.getUserName(), laoCrns)
        .access.associateBy(keySelector = { it.crn })
    } else {
      emptyMap()
    }

    return caseSummaries.map { caseSummary ->
      val crn = caseSummary.crn
      val userAccess = userAccessByCrn[crn]

      when {
        userAccess?.isLimited() == true -> OffenderInfoResult.Limited(crn)
        else -> OffenderInfoResult.Full(crn, caseSummary)
      }
    }
  }

  fun getRiskSummary(crn: String) = try {
    arnsClient.rosh(crn).summary.overallRiskLevel.toString()
  } catch (_: WebClientResponseException.NotFound) {
    throw NotFoundException("CRN", crn)
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
