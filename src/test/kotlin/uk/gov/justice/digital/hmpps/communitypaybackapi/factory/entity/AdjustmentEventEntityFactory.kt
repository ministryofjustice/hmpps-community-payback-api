package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity

import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventAdjustmentType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalDate
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.collections.random

fun AdjustmentEventEntity.Companion.valid() = AdjustmentEventEntity(
  id = UUID.randomUUID(),
  eventType = AdjustmentEventType.entries.toTypedArray().random(),
  triggeredAt = OffsetDateTime.now(),
  triggerType = AdjustmentEventTriggerType.APPOINTMENT_TASK,
  triggeredBy = String.random(20),
  deliusAdjustmentId = Long.random(),
  appointment = AppointmentEntity.valid(),
  adjustmentType = AdjustmentEventAdjustmentType.entries.random(),
  adjustmentMinutes = Int.random(0, 200),
  adjustmentDate = randomLocalDate(),
  adjustmentReason = AdjustmentReasonEntity.valid(),
)

fun AdjustmentEventEntity.Companion.valid(ctx: ApplicationContext) = AdjustmentEventEntity.valid().copy(
  adjustmentReason = ctx.getBean<AdjustmentReasonEntityRepository>().findAll().minByOrNull { it.name }!!,
  appointment = ctx.getBean<AppointmentEntityRepository>().save(AppointmentEntity.valid()),
)
