package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Appointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.AppointmentBehaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.AppointmentSupervisor
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.AppointmentWorkQuality
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ContactOutcome
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.EnforcementAction
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAppointmentPickUp
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Project
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectType
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Provider
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Team
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomHourMinuteDuration
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalDate
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalTime
import java.util.UUID

fun Appointment.Companion.validNoOutcome() = Appointment.valid().copy(
  outcome = null,
  enforcementAction = null,
  penaltyHours = null,
  hiVisWorn = null,
  workedIntensively = null,
  workQuality = null,
  behaviour = null,
)

fun Appointment.Companion.valid() = Appointment(
  id = Long.random(),
  version = UUID.randomUUID(),
  project = Project.valid(),
  projectType = ProjectType.valid(),
  case = CaseSummary.valid(),
  event = NDEvent.valid(),
  team = Team.valid(),
  provider = Provider.valid(),
  pickUpData = NDAppointmentPickUp.valid(),
  date = randomLocalDate(),
  startTime = randomLocalTime(),
  endTime = randomLocalTime(),
  penaltyHours = randomHourMinuteDuration(),
  supervisor = AppointmentSupervisor.valid(),
  outcome = ContactOutcome.valid(),
  enforcementAction = EnforcementAction.valid(),
  hiVisWorn = Boolean.random(),
  workedIntensively = Boolean.random(),
  workQuality = AppointmentWorkQuality.entries.toTypedArray().random(),
  behaviour = AppointmentBehaviour.entries.toTypedArray().random(),
  notes = String.random(),
  sensitive = Boolean.random(),
  alertActive = Boolean.random(),
)
