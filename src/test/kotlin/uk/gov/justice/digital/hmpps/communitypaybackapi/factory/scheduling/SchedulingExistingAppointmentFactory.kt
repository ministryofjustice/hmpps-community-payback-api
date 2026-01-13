package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.scheduling

import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomDuration
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalDate
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalTime
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingExistingAppointment
import java.util.UUID

fun SchedulingExistingAppointment.Companion.valid() = SchedulingExistingAppointment(
  id = UUID.randomUUID(),
  projectCode = String.random(5),
  date = randomLocalDate(),
  startTime = randomLocalTime(),
  endTime = randomLocalTime(),
  hasOutcome = Boolean.random(),
  timeCredited = randomDuration(),
  allocationId = Long.random(),
)
