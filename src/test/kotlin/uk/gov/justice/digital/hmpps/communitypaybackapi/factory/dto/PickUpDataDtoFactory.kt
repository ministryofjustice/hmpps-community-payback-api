package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto

import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.LocationDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.PickUpDataDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.PickUpLocationDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalTime

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
  pickupLocation = PickUpLocationDto.valid(),
  time = randomLocalTime(),
)
