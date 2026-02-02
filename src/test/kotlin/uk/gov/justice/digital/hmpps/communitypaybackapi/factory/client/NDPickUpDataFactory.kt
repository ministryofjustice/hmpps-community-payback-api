package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAddress
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAppointmentPickUp
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCode
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalTime

fun NDAppointmentPickUp.Companion.valid() = NDAppointmentPickUp(
  location = NDAddress.valid(),
  locationCode = NDCode(String.random(5)),
  time = randomLocalTime(),
)
