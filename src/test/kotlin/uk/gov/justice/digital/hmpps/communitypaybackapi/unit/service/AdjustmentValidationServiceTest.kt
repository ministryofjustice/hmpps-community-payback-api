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
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UnpaidWorkDetailsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UnpaidWorkDetailsIdDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.BadRequestException
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdjustmentValidationService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.OffenderService
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockKExtension::class)
class AdjustmentValidationServiceTest {

  @RelaxedMockK
  private lateinit var offenderService: OffenderService

  @RelaxedMockK
  private lateinit var adjustmentReasonEntityRepository: AdjustmentReasonEntityRepository

  @RelaxedMockK
  private lateinit var appointmentEntityRepository: AppointmentEntityRepository

  @InjectMockKs
  private lateinit var service: AdjustmentValidationService

  companion object {
    const val CRN: String = "CRN123"
    const val EVENT_NUMBER: Int = 68
    val UNPAID_WORK_DETAILS: UnpaidWorkDetailsIdDto = UnpaidWorkDetailsIdDto(CRN, EVENT_NUMBER)
    const val USERNAME = "username"
    val REASON_ID: UUID = UUID.fromString("74f0f62b-bbd4-49a4-9af8-1ce6cd94e3e1")
    val APPOINTMENT_ID: UUID = UUID.fromString("84f0f62b-bbd4-49a4-9af8-1ce6cd94e3e1")
  }

  @Nested
  inner class CreateAdjustment {

    private val reason = AdjustmentReasonEntity.valid().copy(id = REASON_ID, maxMinutesAllowed = 180, needsLinkToAppointment = true)
    val baselineRequest = CreateAdjustmentDto.valid().copy(
      adjustmentReasonId = reason.id,
      minutes = 50,
      appointmentId = APPOINTMENT_ID,
    )

    @BeforeEach
    fun setupBaselineMocks() {
      every {
        adjustmentReasonEntityRepository.findByIdOrNull(REASON_ID)
      } returns reason

      every { appointmentEntityRepository.findByIdOrNull(APPOINTMENT_ID) } returns AppointmentEntity.valid()
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
    fun `If adjustment reason needs an appointment and appointment not found return bad request exception`() {
      every { adjustmentReasonEntityRepository.findByIdOrNull(REASON_ID) } returns reason.copy(needsLinkToAppointment = true)

      every { appointmentEntityRepository.findByIdOrNull(APPOINTMENT_ID) } returns null

      assertThatThrownBy {
        service.validateCreate(
          createAdjustment = baselineRequest,
          upwDetailsId = UNPAID_WORK_DETAILS,
          username = USERNAME,
        )
      }.isInstanceOf(BadRequestException::class.java).hasMessage("Appointment not found for ID '84f0f62b-bbd4-49a4-9af8-1ce6cd94e3e1'")
    }

    @Test
    fun `If adjustment reason needs an appointment and appointment ID is null return bad request exception`() {
      every { adjustmentReasonEntityRepository.findByIdOrNull(REASON_ID) } returns reason.copy(name = "The reason name", needsLinkToAppointment = true)

      assertThatThrownBy {
        service.validateCreate(
          createAdjustment = baselineRequest.copy(appointmentId = null),
          upwDetailsId = UNPAID_WORK_DETAILS,
          username = USERNAME,
        )
      }.isInstanceOf(BadRequestException::class.java).hasMessage("Adjustment reason 'The reason name' needs an appointment ID")
    }

    @Test
    fun `If adjustment reason does not need an appointment and appointment ID is not null return bad request exception`() {
      every { adjustmentReasonEntityRepository.findByIdOrNull(REASON_ID) } returns reason.copy(name = "The reason name", needsLinkToAppointment = false)

      assertThatThrownBy {
        service.validateCreate(
          createAdjustment = baselineRequest,
          upwDetailsId = UNPAID_WORK_DETAILS,
          username = USERNAME,
        )
      }.isInstanceOf(BadRequestException::class.java).hasMessage("Adjustment reason 'The reason name' does not support linking to appointments")
    }

    @Test
    fun `If minutes more than allowed for adjustment reason return bad request exception`() {
      every {
        adjustmentReasonEntityRepository.findByIdOrNull(REASON_ID)
      } returns reason.copy(
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
    fun `If minutes more than remaining time required return bad request exception`() {
      val details = UnpaidWorkDetailsDto.valid().copy(
        requiredMinutes = 240,
        completedMinutes = 120,
        adjustments = 0,
      )
      every { offenderService.ensureUnpaidWorkDetailsExist(any(), any()) } returns details

      assertThatThrownBy {
        service.validateCreate(
          createAdjustment = baselineRequest.copy(
            minutes = 180,
          ),
          upwDetailsId = UNPAID_WORK_DETAILS,
          username = USERNAME,
        )
      }.isInstanceOf(BadRequestException::class.java)
        .hasMessage("Credited minutes of '3 hours 0 minutes' exceeds the remaining time required of '2 hours 0 minutes'")
    }

    @Test
    fun `If adjustment date is in the future then return bad request exception`() {
      every { offenderService.ensureUnpaidWorkDetailsExist(any(), any()) } returns UnpaidWorkDetailsDto.valid().copy(
        requiredMinutes = 100,
        completedMinutes = 0,
        adjustments = 0,
      )

      assertThatThrownBy {
        service.validateCreate(
          createAdjustment = baselineRequest.copy(
            adjustmentDate = LocalDate.now().plusDays(1),
          ),
          upwDetailsId = UNPAID_WORK_DETAILS,
          username = USERNAME,
        )
      }.isInstanceOf(BadRequestException::class.java)
        .hasMessage("Adjustment date must not be in the future")
    }

    @Test
    fun `If adjustment date is before the sentence date then return bad request exception`() {
      every { offenderService.ensureUnpaidWorkDetailsExist(any(), any()) } returns UnpaidWorkDetailsDto.valid().copy(
        requiredMinutes = 100,
        completedMinutes = 0,
        adjustments = 0,
        sentenceDate = LocalDate.now().minusMonths(1),
      )

      assertThatThrownBy {
        service.validateCreate(
          createAdjustment = baselineRequest.copy(adjustmentDate = LocalDate.now().minusMonths(1).minusDays(1)),
          upwDetailsId = UNPAID_WORK_DETAILS,
          username = USERNAME,
        )
      }.isInstanceOf(BadRequestException::class.java)
        .hasMessage("Adjustment date must not be before the sentence date")
    }

    @Test
    fun success() {
      every { offenderService.ensureUnpaidWorkDetailsExist(any(), any()) } returns UnpaidWorkDetailsDto.valid().copy(
        requiredMinutes = 100,
        completedMinutes = 0,
        adjustments = 0,
      )

      service.validateCreate(
        createAdjustment = baselineRequest,
        upwDetailsId = UNPAID_WORK_DETAILS,
        username = USERNAME,
      )
    }
  }
}
