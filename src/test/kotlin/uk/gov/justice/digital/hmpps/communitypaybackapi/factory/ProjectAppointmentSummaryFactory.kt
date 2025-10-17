package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAppointmentSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.RequirementProgress

fun ProjectAppointmentSummary.Companion.valid() = ProjectAppointmentSummary(
  id = Long.random(),
  case = CaseSummary.valid(),
  requirementProgress = RequirementProgress.valid(),
)
