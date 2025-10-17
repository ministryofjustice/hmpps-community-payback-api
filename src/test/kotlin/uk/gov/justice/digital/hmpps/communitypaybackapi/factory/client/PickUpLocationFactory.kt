package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.PickUpLocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import kotlin.String

fun PickUpLocation.Companion.valid() = PickUpLocation(
  buildingName = String.Companion.random(),
  buildingNumber = Int.random(1, 200).toString(),
  streetName = String.random(),
  townCity = String.random(),
  county = String.random(),
  postCode = String.random(),
)
