package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import io.mockk.verifyOrder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAdjustment
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEntityUpsertRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdjustmentPersistenceService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toAdjustmentType
import java.util.UUID

@ExtendWith(MockKExtension::class)
class AdjustmentPersistenceServiceTest {
  @RelaxedMockK
  lateinit var adjustmentEntityRepository: AdjustmentEntityRepository

  @RelaxedMockK
  lateinit var adjustmentEntityUpsertRepository: AdjustmentEntityUpsertRepository

  @RelaxedMockK
  lateinit var adjustmentEventEntityRepository: AdjustmentEventEntityRepository

  @RelaxedMockK
  lateinit var adjustmentReasonEntityRepository: AdjustmentReasonEntityRepository

  @InjectMockKs
  lateinit var service: AdjustmentPersistenceService

  @BeforeEach
  fun setupRepositories() {
    every { adjustmentEventEntityRepository.findAllById(any()) } returns emptyList()
    every { adjustmentReasonEntityRepository.findByDeliusCodeIn(any()) } returns emptyList()
    every { adjustmentEntityUpsertRepository.upsert(any()) } answers { firstArg<AdjustmentEntity>().id }
  }

  @Test
  fun `imports Delius fields and the local reason using the canonical stored reference`() {
    val adjustmentFromDelius = NDAdjustment.valid().copy(reference = null)
    val reason = AdjustmentReasonEntity.valid().copy(deliusCode = adjustmentFromDelius.reason.code)
    val canonicalReference = UUID.randomUUID()
    every { adjustmentReasonEntityRepository.findByDeliusCodeIn(listOf(reason.deliusCode)) } returns listOf(reason)
    every { adjustmentEntityUpsertRepository.upsert(any()) } returns canonicalReference

    val result = service.saveAdjustments("X123456", 92, listOf(adjustmentFromDelius)).single()

    assertThat(result.id).isEqualTo(canonicalReference)
    assertThat(result.reason).isEqualTo(reason.name)
    assertThat(result.reasonCode).isEqualTo(reason.deliusCode)
    verify {
      adjustmentEntityUpsertRepository.upsert(
        match {
          it.deliusAdjustmentId == adjustmentFromDelius.id &&
            it.crn == "X123456" &&
            it.deliusEventNumber == 92 &&
            it.adjustmentDate == adjustmentFromDelius.date &&
            it.adjustmentType == adjustmentFromDelius.type.toAdjustmentType() &&
            it.minutes == adjustmentFromDelius.minutes &&
            it.adjustmentReason == reason &&
            it.appointment == null
        },
      )
    }
  }

  @Test
  fun `writes in Delius ID order to avoid deadlocks and returns adjustments in the original order`() {
    val adjustmentsFromDelius = listOf(NDAdjustment.valid().copy(id = 20), NDAdjustment.valid().copy(id = 10))

    val result = service.saveAdjustments("X123456", 92, adjustmentsFromDelius)

    assertThat(result.map { it.deliusId }).containsExactly(20L, 10L)
    verifyOrder {
      adjustmentEntityRepository.flush()
      adjustmentEntityUpsertRepository.upsert(match { it.deliusAdjustmentId == 10L })
      adjustmentEntityUpsertRepository.upsert(match { it.deliusAdjustmentId == 20L })
    }
  }

  @ParameterizedTest
  @CsvSource("CREATE,true", "CREATE,false", "DELETE,true", "DELETE,false")
  fun `recovers appointments only from matching Community Payback creation events`(eventType: AdjustmentEventType, hasAppointment: Boolean) {
    val adjustmentFromDelius = NDAdjustment.valid().copy(id = 100L)
    val historicalEvent = AdjustmentEventEntity.valid().copy(
      id = adjustmentFromDelius.reference!!,
      deliusAdjustmentId = 99L,
      eventType = eventType,
      appointment = AppointmentEntity.valid().takeIf { hasAppointment },
    )
    every { adjustmentEventEntityRepository.findAllById(listOf(historicalEvent.id)) } returns listOf(historicalEvent)

    service.saveAdjustments("X123456", 92, listOf(adjustmentFromDelius))

    verify {
      adjustmentEntityUpsertRepository.upsert(
        match {
          it.id == historicalEvent.id &&
            it.deliusAdjustmentId == adjustmentFromDelius.id &&
            it.appointment == historicalEvent.appointment.takeIf { eventType == AdjustmentEventType.CREATE }
        },
      )
    }
  }

  @Test
  fun `uses the validated appointment when creating an adjustment`() {
    val adjustment = NDAdjustment.valid()
    val appointment = AppointmentEntity.valid()
    val conflictingEvent = AdjustmentEventEntity.valid().copy(id = adjustment.reference!!, eventType = AdjustmentEventType.CREATE, appointment = AppointmentEntity.valid())
    every { adjustmentEventEntityRepository.findAllById(listOf(conflictingEvent.id)) } returns listOf(conflictingEvent)

    service.saveAdjustments("X123456", 92, listOf(adjustment), appointment)

    verify { adjustmentEntityUpsertRepository.upsert(match { it.appointment == appointment }) }
  }
}
