package uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAddress
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDPickUpLocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.LocationDto

fun NDAddress.toDto() = LocationDto(
  buildingName = this.buildingName,
  buildingNumber = this.addressNumber,
  streetName = this.streetName,
  townCity = this.townCity,
  county = this.county,
  postCode = this.postCode,
)

fun NDPickUpLocation.toDto() = LocationDto(
  buildingName = this.buildingName,
  buildingNumber = this.addressNumber,
  streetName = this.streetName,
  townCity = this.townCity,
  county = this.county,
  postCode = this.postCode,
)
