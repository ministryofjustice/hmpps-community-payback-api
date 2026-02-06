package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

data class BeneficiaryDetailsDto(
  val beneficiary: String,
  val contactName: String,
  val emailAddress: String?,
  val website: String?,
  val telephoneNumber: String,
  val location: LocationDto,
)
