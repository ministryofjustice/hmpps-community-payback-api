package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ArnsClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.NotFoundException

@Service
class OffenderService(
  val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
  val arnsClient: ArnsClient,
) {
  fun getRiskSummary(crn: String) = try {
    arnsClient.rosh(crn).summary.overallRiskLevel.toString()
  } catch (_: WebClientResponseException.NotFound) {
    throw NotFoundException("CRN", crn)
  }
}

sealed interface OffenderInfoResult {
  val crn: String

  data class Full(override val crn: String, val summary: CaseSummary) : OffenderInfoResult {
    companion object
  }
  data class Limited(override val crn: String) : OffenderInfoResult
  data class NotFound(override val crn: String) : OffenderInfoResult
}
