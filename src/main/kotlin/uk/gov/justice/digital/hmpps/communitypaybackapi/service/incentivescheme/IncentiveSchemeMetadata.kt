package uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme

data class IncentiveSchemeMetadata(
  val qualifyingThresholdPercentage: String,
  val discountPercentage: String,
  val maximumDiscountPercentage: String,
)
