package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.DeliusAppointmentIdDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeResultDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeResultType
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentsOutcomesResultDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.BadRequestException
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.ConflictException
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.SentryService

@Service
class AppointmentBulkUpdateService(
  private val appointmentUpdateValidationService: AppointmentValidationService,
  private val appointmentRetrievalService: AppointmentRetrievalService,
  private val appointmentUpdateService: AppointmentUpdateService,
  private val sentryService: SentryService,
) {

  fun updateAppointments(
    projectCode: String,
    request: UpdateAppointmentOutcomesDto,
    trigger: AppointmentEventTrigger,
  ): UpdateAppointmentsOutcomesResultDto = UpdateAppointmentsOutcomesResultDto(
    results = request.updates.map { update -> updateAppointment(projectCode, update, trigger) },
  )

  @SuppressWarnings("TooGenericExceptionCaught")
  private fun updateAppointment(
    projectCode: String,
    update: UpdateAppointmentOutcomeDto,
    trigger: AppointmentEventTrigger,
  ): UpdateAppointmentOutcomeResultDto {
    val id = DeliusAppointmentIdDto(projectCode, update.deliusId)
    val existingAppointment = appointmentRetrievalService.getAppointment(id)
      ?: return result(id, UpdateAppointmentOutcomeResultType.NOT_FOUND)

    return try {
      appointmentUpdateValidationService.validateUpdate(existingAppointment, update)
      appointmentUpdateService.updateAppointment(
        existingAppointment = existingAppointment,
        update = update,
        trigger = trigger,
      )
      result(id, UpdateAppointmentOutcomeResultType.SUCCESS)
    } catch (e: BadRequestException) {
      result(id, UpdateAppointmentOutcomeResultType.VALIDATION_ERROR, e.message)
    } catch (_: ConflictException) {
      result(id, UpdateAppointmentOutcomeResultType.VERSION_CONFLICT)
    } catch (t: Throwable) {
      sentryService.captureException(t)
      result(id, UpdateAppointmentOutcomeResultType.SERVER_ERROR)
    }
  }

  private fun result(
    id: DeliusAppointmentIdDto,
    type: UpdateAppointmentOutcomeResultType,
    message: String? = null,
  ) = UpdateAppointmentOutcomeResultDto(id.deliusAppointmentId, type, message)
}
