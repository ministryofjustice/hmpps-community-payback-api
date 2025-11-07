package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Address
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.PickUpData
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalTime

fun PickUpData.Companion.valid() = PickUpData(
  pickUpLocation = Address.Companion.valid(),
  time = randomLocalTime(),
)
