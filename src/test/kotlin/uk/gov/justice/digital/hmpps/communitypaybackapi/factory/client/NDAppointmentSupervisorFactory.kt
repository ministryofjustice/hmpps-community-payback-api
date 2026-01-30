package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAppointmentSupervisor
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDName
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random

fun NDAppointmentSupervisor.Companion.valid() = NDAppointmentSupervisor(
  code = String.random(10),
  name = NDName.valid(),
)
