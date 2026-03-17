package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCodeDescription
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProjectAppointmentSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random

fun NDProjectAppointmentSummary.Companion.valid() = NDProjectAppointmentSummary(
  name = String.random(20),
  code = String.random(20),
  projectType = NDCodeDescription.valid(),
)
