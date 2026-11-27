package uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivesscheme.internal

import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivesscheme.IncentivesSchemeStatus
import java.time.Duration

data class IncentivesSchemeProgression(
  val qualifyingTimeWorked: Duration,
  val status: IncentivesSchemeStatus,
) {
  companion object {
    val INELIGIBLE = IncentivesSchemeProgression(Duration.ZERO, IncentivesSchemeStatus.INELIGIBLE)
  }
}
