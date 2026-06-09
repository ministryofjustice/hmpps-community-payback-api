package uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme

import java.time.Duration

data class IncentiveSchemeDetails(
  val totalRequirement: Duration,
  val qualifyingThreshold: Duration,
  val qualifyingTimeWorked: Duration,
  val totalTimeWorked: Duration,
  val projectedDiscount: Duration,
  val currentDiscount: Duration,
  val status: IncentiveSchemeStatus,
)
