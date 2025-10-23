package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

data class ProjectLocationDto(
  val buildingName: String? = null,
  val addressNumber: String? = null,
  val streetName: String? = null,
  val townCity: String? = null,
  val county: String? = null,
  val postCode: String? = null,
)
