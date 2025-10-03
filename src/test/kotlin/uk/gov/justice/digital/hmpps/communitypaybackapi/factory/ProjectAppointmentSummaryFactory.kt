package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAppointmentSummary

fun ProjectAppointmentSummary.Companion.valid() = ProjectAppointmentSummary(
  id = Long.random(),
  crn = String.random(),
  requirementMinutes = Int.random(0, 100),
  completedMinutes = Int.random(0, 100),
)
