package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdjustmentEventEntityFactory
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdjustmentEventTrigger
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.AdjustmentCreatedEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.AdjustmentDeletedEvent
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class AdjustmentEventEntityFactoryTest {

  @Test
  fun `builds expected adjustment created event`() {
    val triggeredAt = OffsetDateTime.now()
    val appointment = AppointmentEntity.valid()
    val reason = AdjustmentReasonEntity.valid()
    val id = UUID.randomUUID()
    val adjustment = AdjustmentEntity(
      id = id,
      deliusId = 2L,
      crn = appointment.crn,
      deliusEventNumber = appointment.deliusEventNumber,
      type = AdjustmentType.NEGATIVE,
      minutes = 61,
      date = LocalDate.of(1971, 8, 23),
      reason = reason,
      appointment = appointment,
    )
    val result = AdjustmentEventEntityFactory().buildAdjustmentCreated(
      AdjustmentCreatedEvent(
        adjustmentEntity = adjustment,
        trigger = AdjustmentEventTrigger(
          triggeredAt = triggeredAt,
          triggerType = AdjustmentEventTriggerType.APPOINTMENT_TASK,
          triggeredBy = "task id",
        ),
      ),
    )

    assertThat(result.eventType).isEqualTo(AdjustmentEventType.CREATE)
    assertThat(result.triggeredAt).isEqualTo(triggeredAt)
    assertThat(result.triggerType).isEqualTo(AdjustmentEventTriggerType.APPOINTMENT_TASK)
    assertThat(result.triggeredBy).isEqualTo("task id")
    assertThat(result.deliusAdjustmentId).isEqualTo(2L)
    assertThat(result.appointment).isEqualTo(appointment)
    assertThat(result.adjustmentType).isEqualTo(AdjustmentType.NEGATIVE)
    assertThat(result.adjustmentMinutes).isEqualTo(61)
    assertThat(result.adjustmentDate).isEqualTo(LocalDate.of(1971, 8, 23))
    assertThat(result.adjustmentReason).isEqualTo(reason)
    assertThat(result.id).isEqualTo(id)
    assertThat(result.adjustment).isEqualTo(adjustment)
    assertThat(result.referencedEvent).isNull()
  }

  @Test
  fun `builds expected adjustment deleted event`() {
    val triggeredAt = OffsetDateTime.now()
    val appointment = AppointmentEntity.valid()
    val reason = AdjustmentReasonEntity.valid()
    val id = UUID.randomUUID()
    val adjustment = AdjustmentEntity(
      id = UUID.randomUUID(),
      deliusId = 2L,
      crn = appointment.crn,
      deliusEventNumber = appointment.deliusEventNumber,
      type = AdjustmentType.NEGATIVE,
      minutes = 61,
      date = LocalDate.of(1971, 8, 23),
      reason = reason,
      appointment = appointment,
    )
    val referencedEvent = AdjustmentEventEntity(
      id = adjustment.id,
      eventType = AdjustmentEventType.CREATE,
      createdAt = OffsetDateTime.now().minusHours(1L),
      triggeredAt = triggeredAt.minusHours(1L),
      triggerType = AdjustmentEventTriggerType.APPOINTMENT_TASK,
      triggeredBy = "another task id",
      deliusAdjustmentId = 2L,
      appointment = appointment,
      adjustmentType = AdjustmentType.NEGATIVE,
      adjustmentMinutes = 61,
      adjustmentDate = LocalDate.of(1971, 8, 23),
      adjustmentReason = reason,
      adjustment = adjustment,
    )

    val result = AdjustmentEventEntityFactory().buildAdjustmentDeleted(
      AdjustmentDeletedEvent(
        id = id,
        eventToDelete = referencedEvent,
        trigger = AdjustmentEventTrigger(
          triggeredAt = triggeredAt,
          triggerType = AdjustmentEventTriggerType.APPOINTMENT_TASK,
          triggeredBy = "task id",
        ),
      ),
    )

    assertThat(result.eventType).isEqualTo(AdjustmentEventType.DELETE)
    assertThat(result.triggeredAt).isEqualTo(triggeredAt)
    assertThat(result.triggerType).isEqualTo(AdjustmentEventTriggerType.APPOINTMENT_TASK)
    assertThat(result.triggeredBy).isEqualTo("task id")
    assertThat(result.deliusAdjustmentId).isEqualTo(2L)
    assertThat(result.appointment).isEqualTo(appointment)
    assertThat(result.adjustmentType).isEqualTo(AdjustmentType.NEGATIVE)
    assertThat(result.adjustmentMinutes).isEqualTo(61)
    assertThat(result.adjustmentDate).isEqualTo(LocalDate.of(1971, 8, 23))
    assertThat(result.adjustmentReason).isEqualTo(reason)
    assertThat(result.id).isEqualTo(id)
    assertThat(result.adjustment).isEqualTo(adjustment)
    assertThat(result.referencedEvent).isEqualTo(referencedEvent)
  }
}
