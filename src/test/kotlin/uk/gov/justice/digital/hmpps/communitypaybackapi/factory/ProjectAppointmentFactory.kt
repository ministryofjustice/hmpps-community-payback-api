package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAppointment

fun ProjectAppointment.Companion.valid() = ProjectAppointment(
  id = Long.random(),
  projectName = String.random(),
  crn = String.random(),
  requirementMinutes = Int.random(0, 100),
  completedMinutes = Int.random(0, 100),
)
