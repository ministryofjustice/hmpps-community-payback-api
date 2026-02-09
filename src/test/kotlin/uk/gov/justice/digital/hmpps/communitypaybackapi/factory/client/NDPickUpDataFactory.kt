package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAppointmentPickUp
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDPickUpLocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalTime

fun NDAppointmentPickUp.Companion.valid() = NDAppointmentPickUp(
  location = NDPickUpLocation.valid(),
  time = randomLocalTime(),
)
