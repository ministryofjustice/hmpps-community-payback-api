package uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.internal

import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.IncentiveSchemeStatus
import java.time.Duration

data class IncentiveSchemeProgression(
  val qualifyingTimeWorked: Duration,
  val status: IncentiveSchemeStatus,
) {
  companion object {
    val INELIGIBLE = IncentiveSchemeProgression(Duration.ZERO, IncentiveSchemeStatus.INELIGIBLE)
  }
}
