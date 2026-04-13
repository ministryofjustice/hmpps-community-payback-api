package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

data class PickUpLocationsDto(
  val locations: List<PickUpLocationDto>,
)

data class PickUpLocationDto(
  val deliusCode: String,
  val description: String,
  val buildingName: String?,
  val buildingNumber: String?,
  val streetName: String?,
  val townCity: String?,
  val county: String?,
  val postCode: String?,
) {
  companion object
}
