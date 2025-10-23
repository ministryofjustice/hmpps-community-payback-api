package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.Behaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EnforcementActionEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.WorkQuality
import java.util.UUID

fun AppointmentOutcomeEntity.Companion.valid(
  contactOutcomeEntity: ContactOutcomeEntity = ContactOutcomeEntity.valid(),
  enforcementActionEntity: EnforcementActionEntity = EnforcementActionEntity.valid(),
) = AppointmentOutcomeEntity(
  id = UUID.randomUUID(),
  appointmentDeliusId = Long.random(),
  deliusVersionToUpdate = UUID.randomUUID(),
  startTime = randomLocalTime(),
  endTime = randomLocalTime(),
  contactOutcome = contactOutcomeEntity,
  supervisorOfficerCode = String.random(length = 3),
  notes = String.random(),
  hiVisWorn = Boolean.random(),
  workedIntensively = Boolean.random(),
  penaltyMinutes = Long.random(),
  workQuality = WorkQuality.entries.toTypedArray().random(),
  behaviour = Behaviour.entries.toTypedArray().random(),
  enforcementAction = enforcementActionEntity,
  respondBy = randomLocalDate(),
  alertActive = Boolean.random(),
  sensitive = Boolean.random(),
)
