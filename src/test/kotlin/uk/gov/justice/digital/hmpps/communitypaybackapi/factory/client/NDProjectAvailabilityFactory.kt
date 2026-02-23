package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProjectAvailability
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingDayOfWeek
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingFrequency
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalDate

fun NDProjectAvailability.Companion.valid() = NDProjectAvailability(
  frequency = NDSchedulingFrequency.entries.random(),
  dayOfWeek = NDSchedulingDayOfWeek.entries.random(),
  startDateInclusive = randomLocalDate(),
  endDateExclusive = randomLocalDate(),
)
