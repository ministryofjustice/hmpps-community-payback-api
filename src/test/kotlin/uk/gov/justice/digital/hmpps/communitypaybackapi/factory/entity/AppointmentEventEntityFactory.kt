package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity

import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.Behaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.WorkQuality
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalDate
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalTime
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomOffsetDateTime
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentEventTrigger
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.collections.random

fun AppointmentEventEntity.Companion.valid(
  contactOutcomeEntity: ContactOutcomeEntity? = ContactOutcomeEntity.valid(),
) = AppointmentEventEntity(
  id = UUID.randomUUID(),
  appointment = AppointmentEntity.valid(),
  communityPaybackAppointmentId = UUID.randomUUID(),
  eventType = AppointmentEventType.entries.toTypedArray().random(),
  triggeredAt = OffsetDateTime.now(),
  triggerType = AppointmentEventTriggerType.USER,
  triggeredBy = String.random(20),
  deliusAppointmentId = Long.Companion.random(),
  priorDeliusVersion = UUID.randomUUID(),
  crn = String.random(5),
  deliusEventNumber = Int.random(0, 50),
  projectCode = String.random(5),
  projectName = String.random(50),
  date = randomLocalDate(),
  startTime = randomLocalTime(),
  endTime = randomLocalTime(),
  pickupLocationCode = String.random(5),
  pickupLocationDescription = String.random(100),
  pickupTime = randomLocalTime(),
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
)

fun AppointmentEventEntity.Companion.valid(ctx: ApplicationContext) = AppointmentEventEntity.valid().copy(
  contactOutcome = ctx.getBean<ContactOutcomeEntityRepository>().findAll().minByOrNull { it.name }!!,
  appointment = ctx.getBean<AppointmentEntityRepository>().save(AppointmentEntity.valid()),
)

fun AppointmentEventTrigger.Companion.valid() = AppointmentEventTrigger(
  triggeredAt = randomOffsetDateTime(),
  triggeredBy = String.random(5),
  triggerType = AppointmentEventTriggerType.entries.toTypedArray().random(),
)
