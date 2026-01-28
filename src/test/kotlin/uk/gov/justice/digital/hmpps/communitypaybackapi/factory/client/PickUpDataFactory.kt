package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Address
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Code
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAppointmentPickUp
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalTime

fun NDAppointmentPickUp.Companion.valid() = NDAppointmentPickUp(
  location = Address.valid(),
  locationCode = Code(String.random(5)),
  time = randomLocalTime(),
)
