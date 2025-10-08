package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAppointmentBehaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAppointmentWorkQuality
import java.util.UUID

fun ProjectAppointment.Companion.valid() = ProjectAppointment(
  id = Long.random(),
  projectName = String.random(),
  projectCode = String.random(),
  projectTypeName = String.random(),
  projectTypeCode = String.random(),
  crn = String.random(),
  supervisingTeam = String.random(),
  date = randomLocalDate(),
  startTime = randomLocalTime(),
  endTime = randomLocalTime(),
  penaltyTime = randomLocalTime(),
  supervisorCode = String.random(),
  contactOutcomeId = UUID.randomUUID(),
  enforcementActionId = UUID.randomUUID(),
  respondBy = randomLocalDate(),
  hiVisWorn = Boolean.random(),
  workedIntensively = Boolean.random(),
  workQuality = ProjectAppointmentWorkQuality.entries.toTypedArray().random(),
  behaviour = ProjectAppointmentBehaviour.entries.toTypedArray().random(),
  notes = String.random(),
)
