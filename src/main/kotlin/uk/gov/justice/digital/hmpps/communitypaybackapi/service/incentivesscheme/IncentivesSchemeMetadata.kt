package uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivesscheme

data class IncentivesSchemeMetadata(
  val qualifyingThresholdPercentage: String,
  val discountPercentage: String,
  val maximumDiscountPercentage: String,
)
