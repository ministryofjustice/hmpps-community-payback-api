package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.Location
import kotlin.String

fun Location.Companion.valid() = Location(
  buildingName = String.random(),
  buildingNumber = Int.random(1, 200).toString(),
  streetName = String.random(),
  townCity = String.random(),
  county = String.random(),
  postCode = String.random(),
)
