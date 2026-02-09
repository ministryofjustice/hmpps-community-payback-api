package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDPickUpLocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random

fun NDPickUpLocation.Companion.valid() = NDPickUpLocation(
  code = String.random(5),
  description = String.random(100),
  buildingName = String.random(),
  addressNumber = Int.random(1, 200).toString(),
  streetName = String.random(),
  townCity = String.random(),
  county = String.random(),
  postCode = String.random(),
)
