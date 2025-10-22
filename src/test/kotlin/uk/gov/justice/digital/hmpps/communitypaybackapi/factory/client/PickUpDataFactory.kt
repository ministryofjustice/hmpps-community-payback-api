package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.PickUpData
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.PickUpLocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalTime

fun PickUpData.Companion.valid() = PickUpData(
  pickUpLocation = PickUpLocation.Companion.valid(),
  time = randomLocalTime(),
)
