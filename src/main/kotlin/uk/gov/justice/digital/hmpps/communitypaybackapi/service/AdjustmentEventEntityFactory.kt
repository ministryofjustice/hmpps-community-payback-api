package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentTypeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventAdjustmentType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.AdjustmentCreatedEvent
import java.time.OffsetDateTime
import java.util.UUID

@Service
class AdjustmentEventEntityFactory {

  fun buildAdjustmentCreated(
    details: AdjustmentCreatedEvent,
  ) = AdjustmentEventEntity(
    id = UUID.randomUUID(),
    eventType = AdjustmentEventType.CREATE,
    triggeredAt = details.trigger.triggeredAt,
    triggerType = details.trigger.triggerType,
    triggeredBy = details.trigger.triggeredBy,
    deliusAdjustmentId = details.deliusAdjustmentId,
    appointment = details.appointmentEntity,
    adjustmentType = when (details.createDto.type) {
      CreateAdjustmentTypeDto.Positive -> AdjustmentEventAdjustmentType.POSITIVE
      CreateAdjustmentTypeDto.Negative -> AdjustmentEventAdjustmentType.NEGATIVE
    },
    adjustmentMinutes = details.createDto.minutes,
    adjustmentDate = details.createDto.dateOfAdjustment,
    adjustmentReason = details.reason,
  )
}

data class AdjustmentEventTrigger(
  val triggeredAt: OffsetDateTime = OffsetDateTime.now(),
  val triggerType: AdjustmentEventTriggerType,
  val triggeredBy: String,
) {
  companion object
}
