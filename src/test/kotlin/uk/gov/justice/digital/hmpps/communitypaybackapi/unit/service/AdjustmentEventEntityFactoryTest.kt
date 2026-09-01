package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentTypeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventAdjustmentType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.valid
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
    val result = AdjustmentEventEntityFactory().buildAdjustmentCreated(
      AdjustmentCreatedEvent(
        createDto = CreateAdjustmentDto.valid().copy(
          type = CreateAdjustmentTypeDto.Negative,
          minutes = 61,
        ),
        appointmentEntity = appointment,
        reason = reason,
        deliusAdjustmentId = 2L,
        trigger = AdjustmentEventTrigger(
          triggeredAt = triggeredAt,
          triggerType = AdjustmentEventTriggerType.APPOINTMENT_TASK,
          triggeredBy = "task id",
        ),
        id = id,
        adjustmentDate = LocalDate.of(1971, 8, 23),
      ),
    )

    assertThat(result.eventType).isEqualTo(AdjustmentEventType.CREATE)
    assertThat(result.triggeredAt).isEqualTo(triggeredAt)
    assertThat(result.triggerType).isEqualTo(AdjustmentEventTriggerType.APPOINTMENT_TASK)
    assertThat(result.triggeredBy).isEqualTo("task id")
    assertThat(result.deliusAdjustmentId).isEqualTo(2L)
    assertThat(result.appointment).isEqualTo(appointment)
    assertThat(result.adjustmentType).isEqualTo(AdjustmentEventAdjustmentType.NEGATIVE)
    assertThat(result.adjustmentMinutes).isEqualTo(61)
    assertThat(result.adjustmentDate).isEqualTo(LocalDate.of(1971, 8, 23))
    assertThat(result.adjustmentReason).isEqualTo(reason)
    assertThat(result.id).isEqualTo(id)
    assertThat(result.referencedEvent).isNull()
  }

  @Test
  fun `builds expected adjustment deleted event`() {
    val triggeredAt = OffsetDateTime.now()
    val appointment = AppointmentEntity.valid()
    val reason = AdjustmentReasonEntity.valid()
    val id = UUID.randomUUID()
    val referencedEvent = AdjustmentEventEntity(
      id = UUID.randomUUID(),
      eventType = AdjustmentEventType.CREATE,
      createdAt = OffsetDateTime.now().minusHours(1L),
      triggeredAt = triggeredAt.minusHours(1L),
      triggerType = AdjustmentEventTriggerType.APPOINTMENT_TASK,
      triggeredBy = "another task id",
      deliusAdjustmentId = 2L,
      appointment = appointment,
      adjustmentType = AdjustmentEventAdjustmentType.NEGATIVE,
      adjustmentMinutes = 61,
      adjustmentDate = LocalDate.of(1971, 8, 23),
      adjustmentReason = reason,
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
    assertThat(result.adjustmentType).isEqualTo(AdjustmentEventAdjustmentType.NEGATIVE)
    assertThat(result.adjustmentMinutes).isEqualTo(61)
    assertThat(result.adjustmentDate).isEqualTo(LocalDate.of(1971, 8, 23))
    assertThat(result.adjustmentReason).isEqualTo(reason)
    assertThat(result.id).isEqualTo(id)
    assertThat(result.referencedEvent).isEqualTo(referencedEvent)
  }
}
