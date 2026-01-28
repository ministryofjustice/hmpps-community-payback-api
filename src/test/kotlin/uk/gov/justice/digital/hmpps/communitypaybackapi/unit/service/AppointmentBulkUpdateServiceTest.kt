package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeResultType
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.BadRequestException
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.ConflictException
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentBulkUpdateService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentEventTrigger
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentOutcomeValidationService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentRetrievalService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentUpdateService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.SentryService

@ExtendWith(MockKExtension::class)
class AppointmentBulkUpdateServiceTest {

  @MockK(relaxed = true)
  private lateinit var appointmentOutcomeValidationService: AppointmentOutcomeValidationService

  @MockK(relaxed = true)
  private lateinit var appointmentRetrievalService: AppointmentRetrievalService

  @MockK(relaxed = true)
  private lateinit var appointmentUpdateService: AppointmentUpdateService

  @MockK(relaxed = true)
  private lateinit var sentryService: SentryService

  @InjectMockKs
  private lateinit var service: AppointmentBulkUpdateService

  private companion object {
    const val PROJECT_CODE = "PROJ123"
    val TRIGGER: AppointmentEventTrigger = AppointmentEventTrigger(AppointmentEventTriggerType.USER, "user1")
  }

  @Nested
  inner class UpdateAppointmentOutcomes {

    @Test
    fun `don't proceed if validation fails`() {
      val appointment1Dto = AppointmentDto.valid()
      val update1 = UpdateAppointmentOutcomeDto.valid()

      val appointment2Dto = AppointmentDto.valid()
      val update2 = UpdateAppointmentOutcomeDto.valid()

      every { appointmentRetrievalService.getAppointment(PROJECT_CODE, update1.deliusId) } returns appointment1Dto
      every { appointmentOutcomeValidationService.ensureUpdateIsValid(appointment1Dto, update1) } just Runs
      every { appointmentRetrievalService.getAppointment(PROJECT_CODE, update2.deliusId) } returns appointment2Dto
      every { appointmentOutcomeValidationService.ensureUpdateIsValid(appointment2Dto, update2) } throws BadRequestException("oh dear")

      assertThatThrownBy {
        service.updateAppointmentOutcomes(
          projectCode = PROJECT_CODE,
          request = UpdateAppointmentOutcomesDto(listOf(update1, update2)),
          trigger = TRIGGER,
        )
      }.isInstanceOf(BadRequestException::class.java)

      verify(exactly = 0) { appointmentUpdateService.updateAppointmentOutcome(any(), any(), any()) }
    }

    @Test
    fun `not found returned as NOT_FOUND`() {
      val appointment1Dto = AppointmentDto.valid()
      val update1 = UpdateAppointmentOutcomeDto.valid().copy(deliusId = 1L)

      every { appointmentRetrievalService.getAppointment(PROJECT_CODE, update1.deliusId) } returns appointment1Dto
      every { appointmentOutcomeValidationService.ensureUpdateIsValid(appointment1Dto, update1) } just Runs
      every { appointmentUpdateService.updateAppointmentOutcome(PROJECT_CODE, update1, TRIGGER) } throws NotFoundException("appointment", "1")

      val result = service.updateAppointmentOutcomes(
        projectCode = PROJECT_CODE,
        request = UpdateAppointmentOutcomesDto(listOf(update1)),
        trigger = TRIGGER,
      )

      assertThat(result.results).hasSize(1)
      assertThat(result.results[0].deliusId).isEqualTo(1L)
      assertThat(result.results[0].result).isEqualTo(UpdateAppointmentOutcomeResultType.NOT_FOUND)
    }

    @Test
    fun `version conflict returned as VERSION_CONFLICT`() {
      val appointment1Dto = AppointmentDto.valid()
      val update1 = UpdateAppointmentOutcomeDto.valid().copy(deliusId = 1L)

      every { appointmentRetrievalService.getAppointment(PROJECT_CODE, update1.deliusId) } returns appointment1Dto
      every { appointmentOutcomeValidationService.ensureUpdateIsValid(appointment1Dto, update1) } just Runs
      every { appointmentUpdateService.updateAppointmentOutcome(PROJECT_CODE, update1, TRIGGER) } throws ConflictException("oh no")

      val result = service.updateAppointmentOutcomes(
        projectCode = PROJECT_CODE,
        request = UpdateAppointmentOutcomesDto(listOf(update1)),
        trigger = TRIGGER,
      )

      assertThat(result.results).hasSize(1)
      assertThat(result.results[0].deliusId).isEqualTo(1L)
      assertThat(result.results[0].result).isEqualTo(UpdateAppointmentOutcomeResultType.VERSION_CONFLICT)
    }

    @Test
    fun `general exception returns SERVER_ERROR and raises sentry alert`() {
      val appointment1Dto = AppointmentDto.valid()
      val update1 = UpdateAppointmentOutcomeDto.valid().copy(deliusId = 1L)

      every { appointmentRetrievalService.getAppointment(PROJECT_CODE, update1.deliusId) } returns appointment1Dto
      every { appointmentOutcomeValidationService.ensureUpdateIsValid(appointment1Dto, update1) } just Runs

      val exceptionReturned = IllegalStateException("oh no")
      every { appointmentUpdateService.updateAppointmentOutcome(PROJECT_CODE, update1, TRIGGER) } throws exceptionReturned

      val result = service.updateAppointmentOutcomes(
        projectCode = PROJECT_CODE,
        request = UpdateAppointmentOutcomesDto(listOf(update1)),
        trigger = TRIGGER,
      )

      assertThat(result.results).hasSize(1)
      assertThat(result.results[0].deliusId).isEqualTo(1L)
      assertThat(result.results[0].result).isEqualTo(UpdateAppointmentOutcomeResultType.SERVER_ERROR)

      verify { sentryService.captureException(exceptionReturned) }
    }

    @Test
    fun `success returned as SUCCESS`() {
      val appointment1Dto = AppointmentDto.valid()
      val update1 = UpdateAppointmentOutcomeDto.valid().copy(deliusId = 1L)

      every { appointmentRetrievalService.getAppointment(PROJECT_CODE, update1.deliusId) } returns appointment1Dto
      every { appointmentOutcomeValidationService.ensureUpdateIsValid(appointment1Dto, update1) } just Runs

      val result = service.updateAppointmentOutcomes(
        projectCode = PROJECT_CODE,
        request = UpdateAppointmentOutcomesDto(listOf(update1)),
        trigger = TRIGGER,
      )

      assertThat(result.results).hasSize(1)
      assertThat(result.results[0].deliusId).isEqualTo(1L)
      assertThat(result.results[0].result).isEqualTo(UpdateAppointmentOutcomeResultType.SUCCESS)

      verify { appointmentUpdateService.updateAppointmentOutcome(PROJECT_CODE, update1, TRIGGER) }
    }

    @Test
    fun `mix of all outcomes`() {
      val update1 = UpdateAppointmentOutcomeDto.valid().copy(deliusId = 1L)
      val update2 = UpdateAppointmentOutcomeDto.valid().copy(deliusId = 2L)
      val update3 = UpdateAppointmentOutcomeDto.valid().copy(deliusId = 3L)
      val update4 = UpdateAppointmentOutcomeDto.valid().copy(deliusId = 4L)

      every { appointmentOutcomeValidationService.ensureUpdateIsValid(any(), any()) } just Runs

      every { appointmentUpdateService.updateAppointmentOutcome(PROJECT_CODE, update1, TRIGGER) } throws NotFoundException("appointment", "1")
      every { appointmentUpdateService.updateAppointmentOutcome(PROJECT_CODE, update2, TRIGGER) } throws ConflictException("oh no")
      every { appointmentUpdateService.updateAppointmentOutcome(PROJECT_CODE, update3, TRIGGER) } throws IllegalStateException("oh no")

      val result = service.updateAppointmentOutcomes(
        projectCode = PROJECT_CODE,
        request = UpdateAppointmentOutcomesDto(listOf(update1, update2, update3, update4)),
        trigger = TRIGGER,
      )

      assertThat(result.results).hasSize(4)
      assertThat(result.results[0].deliusId).isEqualTo(1L)
      assertThat(result.results[0].result).isEqualTo(UpdateAppointmentOutcomeResultType.NOT_FOUND)
      assertThat(result.results[1].deliusId).isEqualTo(2L)
      assertThat(result.results[1].result).isEqualTo(UpdateAppointmentOutcomeResultType.VERSION_CONFLICT)
      assertThat(result.results[2].deliusId).isEqualTo(3L)
      assertThat(result.results[2].result).isEqualTo(UpdateAppointmentOutcomeResultType.SERVER_ERROR)
      assertThat(result.results[3].deliusId).isEqualTo(4L)
      assertThat(result.results[3].result).isEqualTo(UpdateAppointmentOutcomeResultType.SUCCESS)
    }
  }
}
