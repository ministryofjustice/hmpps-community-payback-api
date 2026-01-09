package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Code
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingAllocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalDate
import java.time.Duration
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.UUID

fun NDSchedulingAppointment.Companion.valid(): NDSchedulingAppointment {
  val startTime = LocalTime.ofSecondOfDay(Long.random(0, 60 * 60 * 12))
  val endTime = startTime.plusSeconds(Long.random(0, 60 * 60 * 12))

  return NDSchedulingAppointment(
    id = UUID.randomUUID(),
    project = NDSchedulingProject.valid(),
    date = randomLocalDate(),
    startTime = startTime,
    endTime = endTime,
    outcome = Code(String.random()),
    timeCredited = Duration.ofMinutes(ChronoUnit.MINUTES.between(startTime, endTime)),
    allocation = NDSchedulingAllocation.valid(),
  )
}

fun NDSchedulingAppointment.Companion.validWithOutcome() = valid()

fun NDSchedulingAppointment.Companion.validWithoutOutcome() = valid().copy(
  outcome = null,
  timeCredited = null,
)
