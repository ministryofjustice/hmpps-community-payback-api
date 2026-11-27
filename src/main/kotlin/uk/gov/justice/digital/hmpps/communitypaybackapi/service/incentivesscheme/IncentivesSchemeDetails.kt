package uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivesscheme

import java.time.Duration

data class IncentivesSchemeDetails(
  val totalRequirement: Duration,
  val qualifyingThreshold: Duration,
  val qualifyingTimeWorked: Duration,
  val totalTimeWorked: Duration,
  val projectedDiscount: Duration,
  val currentDiscount: Duration,
  val status: IncentivesSchemeStatus,
)
