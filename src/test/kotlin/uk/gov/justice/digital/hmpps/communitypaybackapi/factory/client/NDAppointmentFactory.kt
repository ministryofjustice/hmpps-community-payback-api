package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAppointmentBehaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAppointmentPickUp
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAppointmentSupervisor
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAppointmentWorkQuality
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDContactOutcome
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDEnforcementAction
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProjectType
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProvider
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDTeam
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomHourMinuteDuration
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalDate
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalTime
import java.util.UUID

fun NDAppointment.Companion.validNoOutcome() = NDAppointment.valid().copy(
  outcome = null,
  enforcementAction = null,
  penaltyHours = null,
  hiVisWorn = null,
  workedIntensively = null,
  workQuality = null,
  behaviour = null,
)

fun NDAppointment.Companion.valid() = NDAppointment(
  id = Long.random(),
  reference = UUID.randomUUID(),
  version = UUID.randomUUID(),
  project = NDProject.valid(),
  projectType = NDProjectType.valid(),
  case = NDCaseSummary.valid(),
  event = NDEvent.valid(),
  team = NDTeam.valid(),
  provider = NDProvider.valid(),
  pickUpData = NDAppointmentPickUp.valid(),
  date = randomLocalDate(),
  startTime = randomLocalTime(),
  endTime = randomLocalTime(),
  penaltyHours = randomHourMinuteDuration(),
  supervisor = NDAppointmentSupervisor.valid(),
  outcome = NDContactOutcome.valid(),
  enforcementAction = NDEnforcementAction.valid(),
  hiVisWorn = Boolean.random(),
  workedIntensively = Boolean.random(),
  workQuality = NDAppointmentWorkQuality.entries.toTypedArray().random(),
  behaviour = NDAppointmentBehaviour.entries.toTypedArray().random(),
  notes = String.random(),
  sensitive = Boolean.random(),
  alertActive = Boolean.random(),
)
