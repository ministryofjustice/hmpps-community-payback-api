package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.PickUpData
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.Project
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAppointmentBehaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAppointmentWorkQuality
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectType
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.Provider
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.Team
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalDate
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalTime
import java.util.UUID

fun ProjectAppointment.Companion.valid() = ProjectAppointment(
  id = Long.Companion.random(),
  project = Project.Companion.valid(),
  projectType = ProjectType.valid(),
  case = CaseSummary.valid(),
  team = Team.valid(),
  provider = Provider.valid(),
  pickUpData = PickUpData.valid(),
  date = randomLocalDate(),
  startTime = randomLocalTime(),
  endTime = randomLocalTime(),
  penaltyTime = randomLocalTime(),
  supervisorOfficerCode = String.random(),
  contactOutcomeId = UUID.randomUUID(),
  enforcementActionId = UUID.randomUUID(),
  respondBy = randomLocalDate(),
  hiVisWorn = Boolean.random(),
  workedIntensively = Boolean.random(),
  workQuality = ProjectAppointmentWorkQuality.entries.toTypedArray().random(),
  behaviour = ProjectAppointmentBehaviour.entries.toTypedArray().random(),
  notes = String.random(),
)
