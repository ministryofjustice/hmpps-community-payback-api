package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.AdjustmentCreatedEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.AdjustmentDeletedEvent
import java.time.OffsetDateTime

@Service
class AdjustmentEventEntityFactory {

  fun buildAdjustmentCreated(
    details: AdjustmentCreatedEvent,
  ) = AdjustmentEventEntity(
    id = details.adjustmentEntity.id,
    eventType = AdjustmentEventType.CREATE,
    triggeredAt = details.trigger.triggeredAt,
    triggerType = details.trigger.triggerType,
    triggeredBy = details.trigger.triggeredBy,
    deliusAdjustmentId = details.adjustmentEntity.deliusId,
    appointment = details.adjustmentEntity.appointment,
    adjustmentType = details.adjustmentEntity.type,
    adjustmentMinutes = details.adjustmentEntity.minutes,
    adjustmentDate = details.adjustmentEntity.date,
    adjustmentReason = details.adjustmentEntity.reason,
    adjustment = details.adjustmentEntity,
  )

  fun buildAdjustmentDeleted(
    deleteDetails: AdjustmentDeletedEvent,
  ) = AdjustmentEventEntity(
    id = deleteDetails.id,
    eventType = AdjustmentEventType.DELETE,
    triggeredAt = deleteDetails.trigger.triggeredAt,
    triggerType = deleteDetails.trigger.triggerType,
    triggeredBy = deleteDetails.trigger.triggeredBy,
    deliusAdjustmentId = deleteDetails.deliusAdjustmentId,
    appointment = deleteDetails.appointment,
    adjustmentType = deleteDetails.adjustmentType,
    adjustmentMinutes = deleteDetails.adjustmentMinutes,
    adjustmentDate = deleteDetails.adjustmentDate,
    adjustmentReason = deleteDetails.adjustmentReason,
    referencedEvent = deleteDetails.eventToDelete,
    adjustment = deleteDetails.adjustment,
  )
}

data class AdjustmentEventTrigger(
  val triggeredAt: OffsetDateTime = OffsetDateTime.now(),
  val triggerType: AdjustmentEventTriggerType,
  val triggeredBy: String,
) {
  companion object
}
