package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.Behaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.WorkQuality
import java.util.UUID
import kotlin.collections.random

fun AppointmentEventEntity.Companion.valid(
  contactOutcomeEntity: ContactOutcomeEntity? = ContactOutcomeEntity.valid(),
) = AppointmentEventEntity(
  id = UUID.randomUUID(),
  communityPaybackAppointmentId = UUID.randomUUID(),
  eventType = AppointmentEventType.entries.toTypedArray().random(),
  deliusAppointmentId = Long.random(),
  priorDeliusVersion = UUID.randomUUID(),
  crn = String.random(5),
  deliusEventNumber = Int.random(0, 50),
  projectCode = String.random(5),
  date = randomLocalDate(),
  startTime = randomLocalTime(),
  endTime = randomLocalTime(),
  pickupLocationCode = null,
  pickupTime = null,
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
  deliusAllocationId = null,
  triggeredBy = null,
)

fun AppointmentEventEntity.Companion.valid(ctx: ApplicationContext) = AppointmentEventEntity.valid().copy(
  contactOutcome = ctx.getBean<ContactOutcomeEntityRepository>().findAll().first(),
)
