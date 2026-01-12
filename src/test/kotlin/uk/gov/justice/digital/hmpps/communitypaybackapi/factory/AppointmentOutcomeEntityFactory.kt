package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import org.springframework.context.ApplicationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.Behaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.WorkQuality
import java.util.UUID

fun AppointmentOutcomeEntity.Companion.valid(
  contactOutcomeEntity: ContactOutcomeEntity = ContactOutcomeEntity.valid(),
) = AppointmentOutcomeEntity(
  id = UUID.randomUUID(),
  appointmentDeliusId = Long.random(),
  deliusVersionToUpdate = UUID.randomUUID(),
  crn = String.random(5),
  deliusEventNumber = Int.random(0, 50),
  startTime = randomLocalTime(),
  endTime = randomLocalTime(),
  contactOutcome = contactOutcomeEntity,
  supervisorOfficerCode = String.random(length = 3),
  notes = String.random(),
  hiVisWorn = Boolean.random(),
  workedIntensively = Boolean.random(),
  penaltyMinutes = Long.random(),
  minutesCredited = Long.random(),
  workQuality = WorkQuality.entries.toTypedArray().random(),
  behaviour = Behaviour.entries.toTypedArray().random(),
  alertActive = Boolean.random(),
  sensitive = Boolean.random(),
)

fun AppointmentOutcomeEntity.Companion.valid(ctx: ApplicationContext) = AppointmentOutcomeEntity.valid().copy(
  contactOutcome = ctx.getBean(ContactOutcomeEntityRepository::class.java).findAll().first(),
)
