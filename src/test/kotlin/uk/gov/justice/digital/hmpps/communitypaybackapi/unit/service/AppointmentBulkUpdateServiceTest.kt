package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.DeliusAppointmentIdDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeResultType
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.BadRequestException
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.ConflictException
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.validUpdateAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentBulkUpdateService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentEventTrigger
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentRetrievalService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentUpdateService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentValidationService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentValidationService.ValidatedAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.SentryService

@ExtendWith(MockKExtension::class)
class AppointmentBulkUpdateServiceTest {

  @MockK(relaxed = true)
  private lateinit var appointmentUpdateValidationService: AppointmentValidationService

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
    val TRIGGER: AppointmentEventTrigger = AppointmentEventTrigger.valid()
  }

  @Nested
  inner class UpdateAppointmentOutcomes {

    @Test
    fun `don't proceed if validation fails`() {
      val appointment1Dto = AppointmentDto.valid()
      val update1 = UpdateAppointmentOutcomeDto.valid()

      val appointment2Dto = AppointmentDto.valid()
      val update2 = UpdateAppointmentOutcomeDto.valid()

      every { appointmentRetrievalService.getAppointment(DeliusAppointmentIdDto(PROJECT_CODE, update1.deliusId)) } returns appointment1Dto
      every { appointmentUpdateValidationService.validateUpdate(appointment1Dto, update1) } returns ValidatedAppointment.validUpdateAppointment().copy(dto = update1)
      every { appointmentRetrievalService.getAppointment(DeliusAppointmentIdDto(PROJECT_CODE, update2.deliusId)) } returns appointment2Dto
      every { appointmentUpdateValidationService.validateUpdate(appointment2Dto, update2) } throws BadRequestException("oh dear")

      assertThatThrownBy {
        service.updateAppointments(
          projectCode = PROJECT_CODE,
          request = UpdateAppointmentOutcomesDto(listOf(update1, update2)),
          trigger = TRIGGER,
        )
      }.isInstanceOf(BadRequestException::class.java)

      verify(exactly = 0) { appointmentUpdateService.updateAppointment(any(), any(), any()) }
    }

    @Test
    fun `not found returned as NOT_FOUND`() {
      val appointment1Dto = AppointmentDto.valid()
      val update1 = UpdateAppointmentOutcomeDto.valid().copy(deliusId = 1L)

      every { appointmentRetrievalService.getAppointment(DeliusAppointmentIdDto(PROJECT_CODE, update1.deliusId)) } returns appointment1Dto
      every { appointmentUpdateValidationService.validateUpdate(appointment1Dto, update1) } returns ValidatedAppointment.validUpdateAppointment().copy(dto = update1)
      every { appointmentUpdateService.updateAppointment(appointment1Dto, update1, TRIGGER) } throws NotFoundException("appointment", "1")

      val result = service.updateAppointments(
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

      every { appointmentRetrievalService.getAppointment(DeliusAppointmentIdDto(PROJECT_CODE, update1.deliusId)) } returns appointment1Dto
      every { appointmentUpdateValidationService.validateUpdate(appointment1Dto, update1) } returns ValidatedAppointment.validUpdateAppointment().copy(dto = update1)
      every { appointmentUpdateService.updateAppointment(appointment1Dto, update1, TRIGGER) } throws ConflictException("oh no")

      val result = service.updateAppointments(
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

      every { appointmentRetrievalService.getAppointment(DeliusAppointmentIdDto(PROJECT_CODE, update1.deliusId)) } returns appointment1Dto
      every { appointmentUpdateValidationService.validateUpdate(appointment1Dto, update1) } returns ValidatedAppointment.validUpdateAppointment().copy(dto = update1)

      val exceptionReturned = IllegalStateException("oh no")
      every { appointmentUpdateService.updateAppointment(appointment1Dto, update1, TRIGGER) } throws exceptionReturned

      val result = service.updateAppointments(
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

      every { appointmentRetrievalService.getAppointment(DeliusAppointmentIdDto(PROJECT_CODE, update1.deliusId)) } returns appointment1Dto
      every { appointmentUpdateValidationService.validateUpdate(appointment1Dto, update1) } returns ValidatedAppointment.validUpdateAppointment().copy(dto = update1)

      val result = service.updateAppointments(
        projectCode = PROJECT_CODE,
        request = UpdateAppointmentOutcomesDto(listOf(update1)),
        trigger = TRIGGER,
      )

      assertThat(result.results).hasSize(1)
      assertThat(result.results[0].deliusId).isEqualTo(1L)
      assertThat(result.results[0].result).isEqualTo(UpdateAppointmentOutcomeResultType.SUCCESS)

      verify { appointmentUpdateService.updateAppointment(appointment1Dto, update1, TRIGGER) }
    }

    @Test
    fun `mix of all outcomes`() {
      val existing1 = AppointmentDto.valid()
      val existing2 = AppointmentDto.valid()
      val existing3 = AppointmentDto.valid()
      val existing4 = AppointmentDto.valid()

      val update1 = UpdateAppointmentOutcomeDto.valid().copy(deliusId = 1L)
      val update2 = UpdateAppointmentOutcomeDto.valid().copy(deliusId = 2L)
      val update3 = UpdateAppointmentOutcomeDto.valid().copy(deliusId = 3L)
      val update4 = UpdateAppointmentOutcomeDto.valid().copy(deliusId = 4L)

      every { appointmentRetrievalService.getAppointment(DeliusAppointmentIdDto(PROJECT_CODE, update1.deliusId)) } returns existing1
      every { appointmentRetrievalService.getAppointment(DeliusAppointmentIdDto(PROJECT_CODE, update2.deliusId)) } returns existing2
      every { appointmentRetrievalService.getAppointment(DeliusAppointmentIdDto(PROJECT_CODE, update3.deliusId)) } returns existing3
      every { appointmentRetrievalService.getAppointment(DeliusAppointmentIdDto(PROJECT_CODE, update4.deliusId)) } returns existing4

      every { appointmentUpdateValidationService.validateUpdate(existing1, update1) } returns ValidatedAppointment.validUpdateAppointment().copy(dto = update1)
      every { appointmentUpdateValidationService.validateUpdate(existing2, update2) } returns ValidatedAppointment.validUpdateAppointment().copy(dto = update2)
      every { appointmentUpdateValidationService.validateUpdate(existing3, update3) } returns ValidatedAppointment.validUpdateAppointment().copy(dto = update3)
      every { appointmentUpdateValidationService.validateUpdate(existing4, update4) } returns ValidatedAppointment.validUpdateAppointment().copy(dto = update4)

      every { appointmentUpdateService.updateAppointment(existing1, update1, TRIGGER) } throws NotFoundException("appointment", "1")
      every { appointmentUpdateService.updateAppointment(existing2, update2, TRIGGER) } throws ConflictException("oh no")
      every { appointmentUpdateService.updateAppointment(existing3, update3, TRIGGER) } throws IllegalStateException("oh no")

      val result = service.updateAppointments(
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
