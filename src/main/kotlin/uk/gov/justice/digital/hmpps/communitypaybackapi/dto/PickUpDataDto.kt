package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import java.time.LocalTime

data class PickUpDataDto(
  val location: LocationDto?,
  val time: LocalTime?,
)

data class LocationDto(
  val buildingName: String?,
  val buildingNumber: String?,
  val streetName: String?,
  val townCity: String?,
  val county: String?,
  val postCode: String?,
)
