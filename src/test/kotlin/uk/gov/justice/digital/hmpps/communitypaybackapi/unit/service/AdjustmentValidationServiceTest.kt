package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UnpaidWorkDetailsIdDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.BadRequestException
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentTaskEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentTaskEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdjustmentValidationService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.OffenderService
import java.util.UUID

@ExtendWith(MockKExtension::class)
class AdjustmentValidationServiceTest {

  @RelaxedMockK
  private lateinit var offenderService: OffenderService

  @RelaxedMockK
  private lateinit var adjustmentReasonEntityRepository: AdjustmentReasonEntityRepository

  @RelaxedMockK
  private lateinit var appointmentTaskEntityRepository: AppointmentTaskEntityRepository

  @InjectMockKs
  private lateinit var service: AdjustmentValidationService

  companion object {
    const val CRN: String = "CRN123"
    const val EVENT_NUMBER: Int = 68
    val UNPAID_WORK_DETAILS: UnpaidWorkDetailsIdDto = UnpaidWorkDetailsIdDto(CRN, EVENT_NUMBER)
    const val USERNAME = "username"
    val REASON_ID: UUID = UUID.fromString("74f0f62b-bbd4-49a4-9af8-1ce6cd94e3e1")
    val TASK_ID: UUID = UUID.fromString("84f0f62b-bbd4-49a4-9af8-1ce6cd94e3e1")
  }

  @Nested
  inner class CreateAdjustment {

    val baselineRequest = CreateAdjustmentDto.valid().copy(
      adjustmentReasonId = REASON_ID,
      minutes = 50,
      taskId = TASK_ID,
    )

    @BeforeEach
    fun setupBaselineMocks() {
      every {
        adjustmentReasonEntityRepository.findByIdOrNull(REASON_ID)
      } returns AdjustmentReasonEntity.valid().copy(maxMinutesAllowed = 50)

      every { appointmentTaskEntityRepository.findByIdOrNull(TASK_ID) } returns AppointmentTaskEntity.valid()
    }

    @Test
    fun `If adjustment reason not found return bad request exception`() {
      every { adjustmentReasonEntityRepository.findByIdOrNull(REASON_ID) } returns null

      assertThatThrownBy {
        service.validateCreate(
          createAdjustment = baselineRequest,
          upwDetailsId = UNPAID_WORK_DETAILS,
          username = USERNAME,
        )
      }.isInstanceOf(BadRequestException::class.java).hasMessage("Adjustment Reason not found for ID '74f0f62b-bbd4-49a4-9af8-1ce6cd94e3e1'")
    }

    @Test
    fun `If task not found return bad request exception`() {
      every { appointmentTaskEntityRepository.findByIdOrNull(TASK_ID) } returns null

      assertThatThrownBy {
        service.validateCreate(
          createAdjustment = baselineRequest,
          upwDetailsId = UNPAID_WORK_DETAILS,
          username = USERNAME,
        )
      }.isInstanceOf(BadRequestException::class.java).hasMessage("Task not found for ID '84f0f62b-bbd4-49a4-9af8-1ce6cd94e3e1'")
    }

    @Test
    fun `If minutes more than allowed for adjustment reason return bad request exception`() {
      every {
        adjustmentReasonEntityRepository.findByIdOrNull(REASON_ID)
      } returns AdjustmentReasonEntity.valid().copy(
        name = "The reason name",
        maxMinutesAllowed = 50,
      )

      assertThatThrownBy {
        service.validateCreate(
          createAdjustment = baselineRequest.copy(
            minutes = 51,
          ),
          upwDetailsId = UNPAID_WORK_DETAILS,
          username = USERNAME,
        )
      }.isInstanceOf(BadRequestException::class.java)
        .hasMessage("Requested adjustment of '0 hours 51 minutes' exceeds the maximum allowed time '0 hours 50 minutes' for adjustment reason 'The reason name'")
    }

    @Test
    fun success() {
      service.validateCreate(
        createAdjustment = baselineRequest,
        upwDetailsId = UNPAID_WORK_DETAILS,
        username = USERNAME,
      )
    }
  }
}
