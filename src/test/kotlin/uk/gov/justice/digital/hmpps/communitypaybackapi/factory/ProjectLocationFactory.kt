package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectLocation
import kotlin.String

fun ProjectLocation.Companion.valid() = ProjectLocation(
  buildingName = String.random(),
  addressNumber = Int.random(1, 200).toString(),
  streetName = String.random(),
  townCity = String.random(),
  county = String.random(),
  postCode = String.random(),
)
