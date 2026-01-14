package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCodeDescription
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDNameCode
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingExistingAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalDate
import java.time.Duration
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.UUID

fun NDSchedulingExistingAppointment.Companion.valid(): NDSchedulingExistingAppointment {
  val startTime = LocalTime.ofSecondOfDay(Long.random(0, 60 * 60 * 12))
  val endTime = startTime.plusSeconds(Long.random(0, 60 * 60 * 12))

  return NDSchedulingExistingAppointment(
    id = UUID.randomUUID(),
    project = NDNameCode(String.random(20), String.random(5)),
    date = randomLocalDate(),
    startTime = startTime,
    endTime = endTime,
    outcome = NDCodeDescription(String.random(), String.random()),
    minutesCredited = Duration.ofMinutes(ChronoUnit.MINUTES.between(startTime, endTime)),
    allocationId = Long.random(),
  )
}

fun NDSchedulingExistingAppointment.Companion.validWithOutcome() = valid()

fun NDSchedulingExistingAppointment.Companion.validWithoutOutcome() = valid().copy(
  outcome = null,
  minutesCredited = null,
)
