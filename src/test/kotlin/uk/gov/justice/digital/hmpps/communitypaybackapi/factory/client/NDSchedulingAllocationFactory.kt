package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCode
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDPickUp
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingAllocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingAvailability
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingDayOfWeek
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingFrequency
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalDate
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalTime
import java.time.LocalTime
import kotlin.Long

fun NDSchedulingAllocation.Companion.valid(): NDSchedulingAllocation {
  val startDateInclusive = randomLocalDate()
  val endDateInclusive = randomLocalDate().plusDays(Long.random(0, 365))
  val startTime = LocalTime.ofSecondOfDay(Long.random(0, 60 * 60 * 12))
  val endTime = startTime.plusSeconds(Long.random(0, 60 * 60 * 12))

  return NDSchedulingAllocation(
    id = Long.random(),
    project = NDSchedulingProject.valid(),
    projectAvailability = NDSchedulingAvailability.valid(),
    frequency = NDSchedulingFrequency.entries.random(),
    dayOfWeek = NDSchedulingDayOfWeek.entries.toTypedArray().random(),
    startDateInclusive = startDateInclusive,
    endDateInclusive = endDateInclusive,
    startTime = startTime,
    endTime = endTime,
    pickUp = NDPickUp(
      location = NDCode(String.random(5)),
      time = randomLocalTime(),
    ),
  )
}
