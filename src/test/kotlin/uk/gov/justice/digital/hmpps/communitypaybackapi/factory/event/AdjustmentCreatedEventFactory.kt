package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.event

import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdjustmentEventTrigger
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.AdjustmentCreatedEvent
import java.time.OffsetDateTime

fun AdjustmentCreatedEvent.Companion.valid() = AdjustmentCreatedEvent(
  createDto = CreateAdjustmentDto.valid(),
  appointmentEntity = AppointmentEntity.valid(),
  reason = AdjustmentReasonEntity.valid(),
  deliusAdjustmentId = Long.random(),
  trigger = AdjustmentEventTrigger.valid(),
)

fun AdjustmentEventTrigger.Companion.valid() = AdjustmentEventTrigger(
  triggeredAt = OffsetDateTime.now(),
  triggerType = AdjustmentEventTriggerType.entries.random(),
  triggeredBy = String.random(10),
)
