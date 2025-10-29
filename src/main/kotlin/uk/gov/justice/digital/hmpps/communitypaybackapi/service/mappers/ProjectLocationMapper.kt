package uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectLocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.LocationDto

fun ProjectLocation.toDto() = LocationDto(
  buildingName = this.buildingName,
  buildingNumber = this.addressNumber,
  streetName = this.streetName,
  townCity = this.townCity,
  county = this.county,
  postCode = this.postCode,
)
