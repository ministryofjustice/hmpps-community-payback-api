package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.validation.ValidationResultItem
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UnpaidWorkDetailsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UnpaidWorkDetailsIdDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdjustmentValidationService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.OffenderService
import uk.gov.justice.digital.hmpps.communitypaybackapi.unit.common.validation.hasError
import uk.gov.justice.digital.hmpps.communitypaybackapi.unit.common.validation.hasNoErrors
import uk.gov.justice.digital.hmpps.communitypaybackapi.unit.common.validation.hasNoWarnings
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
    val REASON_ID: UUID = UUID.randomUUID()
    val APPOINTMENT_ID: UUID = UUID.randomUUID()
  }

  private val reason = AdjustmentReasonEntity.valid().copy(id = REASON_ID, maxMinutesAllowed = 180, needsLinkToAppointment = true)
  private val baselineRequest = CreateAdjustmentDto.valid().copy(
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
  fun `If adjustment reason not found return error`() {
    every { adjustmentReasonEntityRepository.findByIdOrNull(REASON_ID) } returns null
    val expectedError = ValidationResultItem(
      "$.adjustmentReasonId",
      "UNKNOWN_ADJUSTMENT_REASON",
      mapOf("id" to REASON_ID),
    )

    val result = service.validate(
      baselineRequest,
      AdjustmentValidationService.AdjustmentValidationContext(
        UNPAID_WORK_DETAILS,
        USERNAME,
      ),
    )

    assertThat(result).hasError(expectedError)
    assertThat(result).hasNoWarnings()
  }

  @Test
  fun `If adjustment reason needs an appointment and appointment not found return error`() {
    every { appointmentEntityRepository.findByIdOrNull(APPOINTMENT_ID) } returns null

    val expectedError = ValidationResultItem(
      "$.appointmentId",
      "UNKNOWN_APPOINTMENT",
      mapOf("id" to APPOINTMENT_ID),
    )

    val result = service.validate(
      baselineRequest,
      AdjustmentValidationService.AdjustmentValidationContext(
        UNPAID_WORK_DETAILS,
        USERNAME,
      ),
    )

    assertThat(result).hasError(expectedError)
    assertThat(result).hasNoWarnings()
  }

  @Test
  fun `If adjustment reason needs an appointment and appointment ID is null return error`() {
    val expectedError = ValidationResultItem(
      "$.appointmentId",
      "ADJUSTMENT_REASON_NEEDS_APPOINTMENT_ID",
      mapOf("reasonName" to reason.name),
    )

    val result = service.validate(
      baselineRequest.copy(appointmentId = null),
      AdjustmentValidationService.AdjustmentValidationContext(
        UNPAID_WORK_DETAILS,
        USERNAME,
      ),
    )

    assertThat(result).hasError(expectedError)
    assertThat(result).hasNoWarnings()
  }

  @Test
  fun `If adjustment reason does not need an appointment and appointment ID is not null return error`() {
    every { adjustmentReasonEntityRepository.findByIdOrNull(REASON_ID) } returns reason.copy(needsLinkToAppointment = false)

    val expectedError = ValidationResultItem(
      "$.appointmentId",
      "ADJUSTMENT_REASON_DOES_NOT_SUPPORT_APPOINTMENTS",
      mapOf("reasonName" to reason.name),
    )

    val result = service.validate(
      baselineRequest,
      AdjustmentValidationService.AdjustmentValidationContext(
        UNPAID_WORK_DETAILS,
        USERNAME,
      ),
    )

    assertThat(result).hasError(expectedError)
    assertThat(result).hasNoWarnings()
  }

  @Test
  fun `If minutes more than allowed for adjustment reason return error`() {
    every {
      adjustmentReasonEntityRepository.findByIdOrNull(REASON_ID)
    } returns reason.copy(maxMinutesAllowed = 50)
    val expectedError = ValidationResultItem(
      "$.minutes",
      "EXCEEDS_MAXIMUM_ALLOWED_TIME",
      mapOf(
        "requestedMinutes" to 51,
        "maxMinutesAllowed" to 50,
        "adjustmentReason" to reason.name,
      ),
    )

    val result = service.validate(
      baselineRequest.copy(
        minutes = 51,
      ),
      AdjustmentValidationService.AdjustmentValidationContext(
        upwDetailsId = UNPAID_WORK_DETAILS,
        username = USERNAME,
      ),
    )

    assertThat(result).hasError(expectedError)
    assertThat(result).hasNoWarnings()
  }

  @Test
  fun `If minutes more than remaining time required return error`() {
    val details = UnpaidWorkDetailsDto.valid().copy(
      requiredMinutes = 240,
      completedMinutes = 120,
      adjustments = 0,
    )
    every { offenderService.ensureUnpaidWorkDetailsExist(any(), any()) } returns details
    val expectedError = ValidationResultItem(
      "$.minutes",
      "EXCEEDS_REMAINING_REQUIREMENT_TIME",
      mapOf(
        "requestedMinutes" to 180.toInt(),
        "remainingMinutes" to 120L,
      ),
    )

    val result = service.validate(
      baselineRequest.copy(
        minutes = 180,
      ),
      AdjustmentValidationService.AdjustmentValidationContext(
        upwDetailsId = UNPAID_WORK_DETAILS,
        username = USERNAME,
      ),
    )

    assertThat(result).hasError(expectedError)
    assertThat(result).hasNoWarnings()
  }

  @Test
  fun `If adjustment date is in the future then return error`() {
    every { offenderService.ensureUnpaidWorkDetailsExist(any(), any()) } returns UnpaidWorkDetailsDto.valid().copy(
      requiredMinutes = 100,
      completedMinutes = 0,
      adjustments = 0,
    )
    val expectedError = ValidationResultItem(
      "$.adjustmentDate",
      "ADJUSTMENT_DATE_IS_IN_FUTURE",
      emptyMap(),
    )

    val result = service.validate(
      baselineRequest.copy(
        adjustmentDate = LocalDate.now().plusDays(1),
      ),
      AdjustmentValidationService.AdjustmentValidationContext(
        upwDetailsId = UNPAID_WORK_DETAILS,
        username = USERNAME,
      ),
    )

    assertThat(result).hasError(expectedError)
    assertThat(result).hasNoWarnings()
  }

  @Test
  fun `If adjustment date is before the sentence date then return error`() {
    every { offenderService.ensureUnpaidWorkDetailsExist(any(), any()) } returns UnpaidWorkDetailsDto.valid().copy(
      requiredMinutes = 100,
      completedMinutes = 0,
      adjustments = 0,
      sentenceDate = LocalDate.now().minusMonths(1),
    )
    val expectedError = ValidationResultItem(
      "$.adjustmentDate",
      "ADJUSTMENT_DATE_IS_BEFORE_SENTENCE_DATE",
      emptyMap(),
    )

    val result = service.validate(
      baselineRequest.copy(adjustmentDate = LocalDate.now().minusMonths(1).minusDays(1)),
      AdjustmentValidationService.AdjustmentValidationContext(
        upwDetailsId = UNPAID_WORK_DETAILS,
        username = USERNAME,
      ),
    )

    assertThat(result).hasError(expectedError)
    assertThat(result).hasNoWarnings()
  }

  @Test
  fun `If unpaid work details not found then return error`() {
    every { offenderService.ensureUnpaidWorkDetailsExist(any(), any()) } returns null
    val expectedError = ValidationResultItem(
      "$",
      "COULD_NOT_FIND_UNPAID_WORK_DETAILS",
      mapOf(
        "crn" to UNPAID_WORK_DETAILS.crn,
        "deliusEventNumber" to UNPAID_WORK_DETAILS.deliusEventNumber,
      ),
    )

    val result = service.validate(
      baselineRequest,
      AdjustmentValidationService.AdjustmentValidationContext(
        upwDetailsId = UNPAID_WORK_DETAILS,
        username = USERNAME,
      ),
    )

    assertThat(result).hasError(expectedError)
    assertThat(result).hasNoWarnings()
  }

  @Test
  fun success() {
    every { offenderService.ensureUnpaidWorkDetailsExist(any(), any()) } returns UnpaidWorkDetailsDto.valid().copy(
      requiredMinutes = 100,
      completedMinutes = 0,
      adjustments = 0,
    )

    val result = service.validate(
      baselineRequest,
      AdjustmentValidationService.AdjustmentValidationContext(
        upwDetailsId = UNPAID_WORK_DETAILS,
        username = USERNAME,
      ),
    )

    assertThat(result).hasNoErrors()
    assertThat(result).hasNoWarnings()
  }
}
