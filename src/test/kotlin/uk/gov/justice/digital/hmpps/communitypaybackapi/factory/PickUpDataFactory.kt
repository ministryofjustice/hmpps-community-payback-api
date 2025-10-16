package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.Location
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.PickUpData

fun PickUpData.Companion.valid() = PickUpData(
  location = Location.valid(),
  time = randomLocalTime(),
)
