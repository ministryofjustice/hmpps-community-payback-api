package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.OffenderInfoResult

fun OffenderInfoResult.Full.Companion.valid(
  crn: String = String.random(8),
) = OffenderInfoResult.Full(
  crn,
  summary = CaseSummary.valid().copy(crn = crn),
)
