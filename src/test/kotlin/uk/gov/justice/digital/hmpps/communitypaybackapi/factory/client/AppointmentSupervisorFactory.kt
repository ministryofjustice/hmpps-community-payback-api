package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.AppointmentSupervisor
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Name
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random

fun AppointmentSupervisor.Companion.valid() = AppointmentSupervisor(
  code = String.random(10),
  name = Name.valid(),
)
