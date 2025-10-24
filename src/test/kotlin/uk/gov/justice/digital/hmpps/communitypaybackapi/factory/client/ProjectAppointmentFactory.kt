package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.AppointmentSupervisor
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ContactOutcome
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.EnforcementAction
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.PickUpData
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Project
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectAppointmentBehaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectAppointmentWorkQuality
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectType
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Provider
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Team
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalDate
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalTime
import java.util.UUID

fun ProjectAppointment.Companion.valid() = ProjectAppointment(
  id = Long.Companion.random(),
  version = UUID.randomUUID(),
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
  supervisor = AppointmentSupervisor.valid(),
  outcome = ContactOutcome.valid(),
  enforcementAction = EnforcementAction.valid(),
  hiVisWorn = Boolean.random(),
  workedIntensively = Boolean.random(),
  workQuality = ProjectAppointmentWorkQuality.entries.toTypedArray().random(),
  behaviour = ProjectAppointmentBehaviour.entries.toTypedArray().random(),
  notes = String.random(),
  sensitive = Boolean.random(),
  alertActive = Boolean.random(),
)
