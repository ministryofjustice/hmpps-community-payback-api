package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentTypeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventAdjustmentType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
import java.time.OffsetDateTime
import java.util.UUID

@Service
class AdjustmentEventEntityFactory {

  fun buildAdjustmentCreated(
    createAdjustmentDto: CreateAdjustmentDto,
    appointment: AppointmentEntity,
    reason: AdjustmentReasonEntity,
    deliusAdjustmentId: Long,
    trigger: AdjustmentEventTrigger,
  ) = AdjustmentEventEntity(
    id = UUID.randomUUID(),
    triggeredAt = trigger.triggeredAt,
    triggerType = trigger.triggerType,
    triggeredBy = trigger.triggeredBy,
    deliusAdjustmentId = deliusAdjustmentId,
    appointment = appointment,
    adjustmentType = when (createAdjustmentDto.type) {
      CreateAdjustmentTypeDto.Positive -> AdjustmentEventAdjustmentType.POSITIVE
      CreateAdjustmentTypeDto.Negative -> AdjustmentEventAdjustmentType.NEGATIVE
    },
    adjustmentMinutes = createAdjustmentDto.minutes,
    adjustmentDate = createAdjustmentDto.dateOfAdjustment,
    adjustmentReason = reason,
  )
}

data class AdjustmentEventTrigger(
  val triggeredAt: OffsetDateTime = OffsetDateTime.now(),
  val triggerType: AdjustmentEventTriggerType,
  val triggeredBy: String,
) {
  companion object
}
