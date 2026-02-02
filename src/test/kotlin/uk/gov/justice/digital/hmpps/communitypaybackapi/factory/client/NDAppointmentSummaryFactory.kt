package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import org.springframework.context.ApplicationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAppointmentSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDContactOutcome
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDRequirementProgress
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random

fun NDAppointmentSummary.Companion.valid() = NDAppointmentSummary(
  id = Long.random(),
  case = NDCaseSummary.valid(),
  outcome = NDContactOutcome.valid(),
  requirementProgress = NDRequirementProgress.Companion.valid(),
)

fun NDAppointmentSummary.Companion.valid(ctx: ApplicationContext) = NDAppointmentSummary.valid().copy(
  outcome = NDContactOutcome.valid(ctx),
)
