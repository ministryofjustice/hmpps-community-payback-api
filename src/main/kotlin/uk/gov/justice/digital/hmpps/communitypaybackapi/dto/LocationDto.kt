package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

data class LocationDto(
  val buildingName: String?,
  val buildingNumber: String?,
  val streetName: String?,
  val townCity: String?,
  val county: String?,
  val postCode: String?,
) {
  companion object
}
