package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.PickUpData
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.PickUpLocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalTime

fun PickUpData.Companion.valid() = PickUpData(
  pickUpLocation = PickUpLocation.Companion.valid(),
  time = randomLocalTime(),
)
