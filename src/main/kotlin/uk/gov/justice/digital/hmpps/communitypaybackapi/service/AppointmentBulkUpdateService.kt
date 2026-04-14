package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.DeliusAppointmentIdDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeResultDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeResultType
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentsOutcomesResultDto
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
  ): UpdateAppointmentsOutcomesResultDto {
    val internalRequests = request.validate(projectCode)
    val outcomes = internalRequests.apply(trigger)
    return UpdateAppointmentsOutcomesResultDto(outcomes)
  }

  private fun UpdateAppointmentOutcomesDto.validate(projectCode: String) = updates.map {
    val id = DeliusAppointmentIdDto(projectCode, it.deliusId)
    val existingAppointment = appointmentRetrievalService.getAppointment(id)

    if (existingAppointment != null) {
      appointmentUpdateValidationService.validateUpdate(existingAppointment, it)
    }

    AppointmentWithUpdateRequest(id, existingAppointment, it)
  }

  @SuppressWarnings("TooGenericExceptionCaught")
  private fun List<AppointmentWithUpdateRequest>.apply(
    trigger: AppointmentEventTrigger,
  ) = map { internalUpdateRequest ->
    val outcome = if (internalUpdateRequest.existingAppointment == null) {
      UpdateAppointmentOutcomeResultType.NOT_FOUND
    } else {
      try {
        appointmentUpdateService.updateAppointment(
          existingAppointment = internalUpdateRequest.existingAppointment,
          update = internalUpdateRequest.update,
          trigger = trigger,
        )
        UpdateAppointmentOutcomeResultType.SUCCESS
      } catch (_: ConflictException) {
        UpdateAppointmentOutcomeResultType.VERSION_CONFLICT
      } catch (t: Throwable) {
        sentryService.captureException(t)
        UpdateAppointmentOutcomeResultType.SERVER_ERROR
      }
    }

    UpdateAppointmentOutcomeResultDto(internalUpdateRequest.id.deliusAppointmentId, outcome)
  }

  private data class AppointmentWithUpdateRequest(
    val id: DeliusAppointmentIdDto,
    val existingAppointment: AppointmentDto?,
    val update: UpdateAppointmentOutcomeDto,
  )
}
