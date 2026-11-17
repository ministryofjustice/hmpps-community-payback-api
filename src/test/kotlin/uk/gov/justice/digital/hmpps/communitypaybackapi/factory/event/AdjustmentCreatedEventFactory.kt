package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.event

import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomPastLocalDate
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdjustmentEventTrigger
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.AdjustmentCreatedEvent
import java.time.OffsetDateTime
import java.util.UUID

fun AdjustmentCreatedEvent.Companion.valid(appointment: AppointmentEntity? = AppointmentEntity.valid()) = AdjustmentCreatedEvent(
  adjustmentEntity = AdjustmentEntity(
    id = UUID.randomUUID(),
    deliusId = Long.random(),
    crn = appointment?.crn ?: String.random(7),
    deliusEventNumber = appointment?.deliusEventNumber ?: 1,
    type = AdjustmentType.POSITIVE,
    minutes = 30,
    date = randomPastLocalDate(),
    reason = AdjustmentReasonEntity.valid(),
    appointment = appointment,
  ),
  trigger = AdjustmentEventTrigger.valid(),
)

fun AdjustmentEventTrigger.Companion.valid() = AdjustmentEventTrigger(
  triggeredAt = OffsetDateTime.now(),
  triggerType = AdjustmentEventTriggerType.entries.random(),
  triggeredBy = String.random(10),
)
