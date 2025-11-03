package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.AppointmentSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ContactOutcome
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.RequirementProgress
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random

fun AppointmentSummary.Companion.valid() = AppointmentSummary(
  id = Long.Companion.random(),
  case = CaseSummary.valid(),
  outcome = ContactOutcome.valid(),
  requirementProgress = RequirementProgress.Companion.valid(),
)
