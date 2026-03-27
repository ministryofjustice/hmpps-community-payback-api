package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentTypeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventAdjustmentType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
import java.time.OffsetDateTime
import java.util.UUID

@Service
class AdjustmentEventEntityFactory {

  fun buildAdjustmentCreated(
    details: CreateAdjustmentEventDetails,
  ) = AdjustmentEventEntity(
    id = UUID.randomUUID(),
    eventType = AdjustmentEventType.CREATE,
    triggeredAt = details.trigger.triggeredAt,
    triggerType = details.trigger.triggerType,
    triggeredBy = details.trigger.triggeredBy,
    deliusAdjustmentId = details.deliusAdjustmentId,
    appointment = details.appointment,
    adjustmentType = when (details.createAdjustmentDto.type) {
      CreateAdjustmentTypeDto.Positive -> AdjustmentEventAdjustmentType.POSITIVE
      CreateAdjustmentTypeDto.Negative -> AdjustmentEventAdjustmentType.NEGATIVE
    },
    adjustmentMinutes = details.createAdjustmentDto.minutes,
    adjustmentDate = details.createAdjustmentDto.dateOfAdjustment,
    adjustmentReason = details.reason,
  )

  data class CreateAdjustmentEventDetails(
    val createAdjustmentDto: CreateAdjustmentDto,
    val appointment: AppointmentEntity,
    val reason: AdjustmentReasonEntity,
    val deliusAdjustmentId: Long,
    val trigger: AdjustmentEventTrigger,
  )
}

data class AdjustmentEventTrigger(
  val triggeredAt: OffsetDateTime = OffsetDateTime.now(),
  val triggerType: AdjustmentEventTriggerType,
  val triggeredBy: String,
) {
  companion object
}
