package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.scheduling

import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomDuration
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulePlan
import java.util.UUID.randomUUID

fun SchedulePlan.Companion.valid() = SchedulePlan(
  schedulingId = randomUUID(),
  crn = String.random(5),
  eventNumber = Int.random(50),
  actions = emptyList(),
  shortfall = randomDuration(),
)
