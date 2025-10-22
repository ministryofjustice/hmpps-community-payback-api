package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectAppointmentSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.RequirementProgress
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random

fun ProjectAppointmentSummary.Companion.valid() = ProjectAppointmentSummary(
  id = Long.Companion.random(),
  case = CaseSummary.valid(),
  requirementProgress = RequirementProgress.Companion.valid(),
)
