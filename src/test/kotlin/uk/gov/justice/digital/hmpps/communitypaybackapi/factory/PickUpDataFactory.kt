package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.PickUpData
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.PickUpLocation

fun PickUpData.Companion.valid() = PickUpData(
  pickUpLocation = PickUpLocation.valid(),
  time = randomLocalTime(),
)
