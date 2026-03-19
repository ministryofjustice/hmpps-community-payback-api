package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import org.springframework.context.ApplicationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAppointmentSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDContactOutcome
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProjectAppointmentSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDRequirementProgress
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalDate
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalTime

fun NDAppointmentSummary.Companion.valid() = NDAppointmentSummary(
  id = Long.random(),
  project = NDProjectAppointmentSummary.valid(),
  case = NDCaseSummary.valid(),
  outcome = NDContactOutcome.valid(),
  requirementProgress = NDRequirementProgress.valid(),
  date = randomLocalDate(),
  startTime = randomLocalTime(),
  endTime = randomLocalTime(),
  minutesCredited = Long.random(0, 2000),
  daysOverdue = Int.random(0, 365),
  notes = String.random(200),
)

fun NDAppointmentSummary.Companion.valid(ctx: ApplicationContext) = NDAppointmentSummary.valid().copy(
  outcome = NDContactOutcome.valid(ctx),
)
