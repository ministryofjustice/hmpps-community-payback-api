package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.PickUpLocation
import kotlin.String

fun PickUpLocation.Companion.valid() = PickUpLocation(
  buildingName = String.random(),
  buildingNumber = Int.random(1, 200).toString(),
  streetName = String.random(),
  townCity = String.random(),
  county = String.random(),
  postCode = String.random(),
)
