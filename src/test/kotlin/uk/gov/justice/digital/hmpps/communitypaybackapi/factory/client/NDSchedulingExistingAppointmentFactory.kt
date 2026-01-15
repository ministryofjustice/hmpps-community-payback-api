package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCodeDescription
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDNameCode
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingExistingAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

fun NDSchedulingExistingAppointment.Companion.valid(): NDSchedulingExistingAppointment {
  val startTime = LocalTime.ofSecondOfDay(Long.random(0, 60 * 60 * 12))
  val endTime = startTime.plusSeconds(Long.random(0, 60 * 60 * 12))

  return NDSchedulingExistingAppointment(
    id = Long.random(),
    project = NDNameCode(String.random(20), String.random(5)),
    date = randomLocalDate(),
    startTime = startTime,
    endTime = endTime,
    outcome = NDCodeDescription(String.random(), String.random()),
    minutesCredited = ChronoUnit.MINUTES.between(startTime, endTime),
    allocationId = Long.random(),
  )
}

fun NDSchedulingExistingAppointment.Companion.validWithOutcome() = valid()
