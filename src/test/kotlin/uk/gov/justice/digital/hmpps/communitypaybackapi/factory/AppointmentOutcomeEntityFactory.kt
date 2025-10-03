package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.AppointmentOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.Behaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.WorkQuality
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.entity.EnforcementActionEntity
import java.util.UUID

fun AppointmentOutcomeEntity.Companion.valid(
  contactOutcomeEntity: ContactOutcomeEntity = ContactOutcomeEntity.valid(),
  enforcementActionEntity: EnforcementActionEntity = EnforcementActionEntity.valid(),
) = AppointmentOutcomeEntity(
  id = UUID.randomUUID(),
  appointmentDeliusId = Long.random(),
  startTime = randomLocalTime(),
  endTime = randomLocalTime(),
  contactOutcomeId = contactOutcomeEntity.id,
  contactOutcomeEntity = contactOutcomeEntity,
  supervisorOfficerCode = String.random(length = 3),
  notes = String.random(),
  hiVisWorn = Boolean.random(),
  workedIntensively = Boolean.random(),
  penaltyMinutes = Long.random(),
  workQuality = WorkQuality.entries.toTypedArray().random(),
  behaviour = Behaviour.entries.toTypedArray().random(),
  enforcementActionId = enforcementActionEntity.id,
  enforcementActionEntity = enforcementActionEntity,
  respondBy = randomLocalDate(),
)
