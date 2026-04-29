package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ArnsClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CaseDetailsSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.OffenderNameDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UnpaidWorkDetailsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UnpaidWorkDetailsIdDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toOffenderNameDto

@Service
class OffenderService(
  val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
  val arnsClient: ArnsClient,
) {
  fun getRiskSummary(crn: String) = try {
    arnsClient.rosh(crn).summary.overallRiskLevel.toString()
  } catch (_: WebClientResponseException.NotFound) {
    null
  }

  fun ensureUnpaidWorkDetailsExist(upwDetailsId: UnpaidWorkDetailsIdDto, userName: String? = null) = getUnpaidWorkDetails(upwDetailsId, userName)

  fun getUnpaidWorkDetails(upwDetailsId: UnpaidWorkDetailsIdDto, userName: String? = null): UnpaidWorkDetailsDto? {
    val (crn, deliusEventNumber) = upwDetailsId
    return getOffenderSummaryByCrn(crn, userName)?.unpaidWorkDetails?.firstOrNull { it.eventNumber == deliusEventNumber }
  }

  fun getOffenderSummaryByCrn(crn: String, userName: String?): CaseDetailsSummaryDto? = try {
    communityPaybackAndDeliusClient.getUpwDetailsSummary(crn, userName).toDto()
  } catch (_: WebClientResponseException.NotFound) {
    return null
  }

  fun getNameIgnoringLimitedStatus(crn: String): OffenderNameDto? = try {
    communityPaybackAndDeliusClient.getUpwDetailsSummary(crn, null).case.toOffenderNameDto()
  } catch (_: WebClientResponseException.NotFound) {
    null
  }
}
