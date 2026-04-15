package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentTypeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventAdjustmentType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdjustmentEventEntityFactory
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdjustmentEventTrigger
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.AdjustmentCreatedEvent
import java.time.LocalDate
import java.time.OffsetDateTime

class AdjustmentEventEntityFactoryTest {

  @Test
  fun success() {
    val triggeredAt = OffsetDateTime.now()
    val appointment = AppointmentEntity.valid()
    val reason = AdjustmentReasonEntity.valid()

    val result = AdjustmentEventEntityFactory().buildAdjustmentCreated(
      AdjustmentCreatedEvent(
        createDto = CreateAdjustmentDto.valid().copy(
          type = CreateAdjustmentTypeDto.Negative,
          minutes = 61,
          dateOfAdjustment = LocalDate.of(1971, 8, 23),
        ),
        appointmentEntity = appointment,
        reason = reason,
        deliusAdjustmentId = 2L,
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
    assertThat(result.adjustmentType).isEqualTo(AdjustmentEventAdjustmentType.NEGATIVE)
    assertThat(result.adjustmentMinutes).isEqualTo(61)
    assertThat(result.adjustmentDate).isEqualTo(LocalDate.of(1971, 8, 23))
    assertThat(result.adjustmentReason).isEqualTo(reason)
  }
}
