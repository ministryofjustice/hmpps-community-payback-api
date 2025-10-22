package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectLocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import kotlin.String

fun ProjectLocation.Companion.valid() = ProjectLocation(
  buildingName = String.Companion.random(),
  addressNumber = Int.random(1, 200).toString(),
  streetName = String.random(),
  townCity = String.random(),
  county = String.random(),
  postCode = String.random(),
)
