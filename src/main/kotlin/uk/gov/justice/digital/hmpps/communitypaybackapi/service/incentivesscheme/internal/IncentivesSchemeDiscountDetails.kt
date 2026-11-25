package uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivesscheme.internal

import java.time.Duration

data class IncentivesSchemeDiscountDetails(
  val current: Duration,
  val projected: Duration,
)
