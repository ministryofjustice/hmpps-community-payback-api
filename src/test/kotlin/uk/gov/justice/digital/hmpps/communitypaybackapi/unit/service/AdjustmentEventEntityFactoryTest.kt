package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentTypeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventAdjustmentType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdjustmentEventEntityFactory
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdjustmentEventTrigger
import java.time.LocalDate
import java.time.OffsetDateTime

class AdjustmentEventEntityFactoryTest {

  @Test
  fun success() {
    val triggeredAt = OffsetDateTime.now()
    val appointment = AppointmentEntity.valid()
    val reason = AdjustmentReasonEntity.valid()

    val result = AdjustmentEventEntityFactory().buildAdjustmentCreated(
      createAdjustmentDto = CreateAdjustmentDto.valid().copy(
        type = CreateAdjustmentTypeDto.Negative,
        minutes = 61,
        dateOfAdjustment = LocalDate.of(1971, 8, 23),
      ),
      appointment = appointment,
      reason = reason,
      deliusAdjustmentId = 2L,
      trigger = AdjustmentEventTrigger(
        triggeredAt = triggeredAt,
        triggerType = AdjustmentEventTriggerType.CREATE,
        triggeredBy = "mr trigger",
      ),
    )

    assertThat(result.triggeredAt).isEqualTo(triggeredAt)
    assertThat(result.triggerType).isEqualTo(AdjustmentEventTriggerType.CREATE)
    assertThat(result.triggeredBy).isEqualTo("mr trigger")
    assertThat(result.deliusAdjustmentId).isEqualTo(2L)
    assertThat(result.appointment).isEqualTo(appointment)
    assertThat(result.adjustmentType).isEqualTo(AdjustmentEventAdjustmentType.NEGATIVE)
    assertThat(result.adjustmentMinutes).isEqualTo(61)
    assertThat(result.adjustmentDate).isEqualTo(LocalDate.of(1971, 8, 23))
    assertThat(result.adjustmentReason).isEqualTo(reason)
  }
}
