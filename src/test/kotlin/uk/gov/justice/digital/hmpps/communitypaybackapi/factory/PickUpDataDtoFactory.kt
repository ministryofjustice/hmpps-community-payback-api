package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.LocationDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.PickUpDataDto

fun PickUpDataDto.Companion.valid() = PickUpDataDto(
  location = LocationDto(
    buildingName = String.random(),
    buildingNumber = String.random(),
    streetName = String.random(),
    townCity = String.random(),
    county = String.random(),
    postCode = String.random(),
  ),
  locationCode = String.random(5),
  locationDescription = String.random(50),
  time = randomLocalTime(),
)
