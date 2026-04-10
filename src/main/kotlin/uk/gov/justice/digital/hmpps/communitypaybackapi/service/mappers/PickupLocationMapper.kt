package uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDPickUpLocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.LocationDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.PickUpLocationDto

fun NDPickUpLocation.toLocationDto() = LocationDto(
  buildingName = this.buildingName,
  buildingNumber = this.addressNumber,
  streetName = this.streetName,
  townCity = this.townCity,
  county = this.county,
  postCode = this.postCode,
)

fun NDPickUpLocation.toDto() = PickUpLocationDto(
  deliusCode = this.code,
  description = this.description,
  buildingName = this.buildingName,
  buildingNumber = this.addressNumber,
  streetName = this.streetName,
  townCity = this.townCity,
  county = this.county,
  postCode = this.postCode,
)
