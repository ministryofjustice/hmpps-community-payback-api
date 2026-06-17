package uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.internal

import java.time.Duration

data class IncentiveSchemeDiscountDetails(
  val current: Duration,
  val projected: Duration,
)
