package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingAvailability
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingFrequency
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalDate

fun NDSchedulingAvailability.Companion.valid() = NDSchedulingAvailability(
  frequency = NDSchedulingFrequency.entries.random(),
  endDateExclusive = randomLocalDate(),
)
