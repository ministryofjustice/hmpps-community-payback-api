package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCreatedAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import java.util.UUID

fun NDCreatedAppointment.Companion.valid() = NDCreatedAppointment(
  id = Long.random(),
  reference = UUID.randomUUID(),
)
