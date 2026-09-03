package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentTypeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventAdjustmentType
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
    id = details.id,
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
    adjustmentDate = details.adjustmentDate,
    adjustmentReason = details.reason,
  )

  fun buildAdjustmentDeleted(
    deleteDetails: AdjustmentDeletedEvent,
  ) = AdjustmentEventEntity(
    id = deleteDetails.id,
    eventType = AdjustmentEventType.DELETE,
    triggeredAt = deleteDetails.trigger.triggeredAt,
    triggerType = deleteDetails.trigger.triggerType,
    triggeredBy = deleteDetails.trigger.triggeredBy,
    deliusAdjustmentId = deleteDetails.eventToDelete.deliusAdjustmentId,
    appointment = deleteDetails.eventToDelete.appointment,
    adjustmentType = deleteDetails.eventToDelete.adjustmentType,
    adjustmentMinutes = deleteDetails.eventToDelete.adjustmentMinutes,
    adjustmentDate = deleteDetails.eventToDelete.adjustmentDate,
    adjustmentReason = deleteDetails.eventToDelete.adjustmentReason,
    referencedEvent = deleteDetails.eventToDelete,
  )
}

data class AdjustmentEventTrigger(
  val triggeredAt: OffsetDateTime = OffsetDateTime.now(),
  val triggerType: AdjustmentEventTriggerType,
  val triggeredBy: String,
) {
  companion object
}
