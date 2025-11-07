package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import org.springframework.context.ApplicationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.AppointmentSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ContactOutcome
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.RequirementProgress
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import java.time.LocalTime

fun AppointmentSummary.Companion.valid() = AppointmentSummary(
  id = Long.Companion.random(),
  startTime = LocalTime.of(9, 0),
  endTime = LocalTime.of(17, 0),
  case = CaseSummary.valid(),
  outcome = ContactOutcome.valid(),
  requirementProgress = RequirementProgress.Companion.valid(),
)

fun AppointmentSummary.Companion.valid(ctx: ApplicationContext) = AppointmentSummary.valid().copy(
  outcome = ContactOutcome.valid(ctx),
)
