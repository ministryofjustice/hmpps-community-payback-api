package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeResultDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeResultType
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentsOutcomesResultDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.ConflictException
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.SentryService

@Service
class AppointmentBulkUpdateService(
  private val appointmentOutcomeValidationService: AppointmentOutcomeValidationService,
  private val appointmentRetrievalService: AppointmentRetrievalService,
  private val appointmentUpdateService: AppointmentUpdateService,
  private val sentryService: SentryService,
) {

  fun updateAppointmentOutcomes(
    projectCode: String,
    request: UpdateAppointmentOutcomesDto,
  ): UpdateAppointmentsOutcomesResultDto {
    request.validate(projectCode)

    val outcomes = request.apply(projectCode)

    return UpdateAppointmentsOutcomesResultDto(outcomes)
  }

  private fun UpdateAppointmentOutcomesDto.validate(projectCode: String) = updates.forEach {
    appointmentOutcomeValidationService.ensureUpdateIsValid(
      appointment = appointmentRetrievalService.getAppointment(
        projectCode,
        it.deliusId,
      ),
      update = it,
    )
  }

  @SuppressWarnings("TooGenericExceptionCaught")
  private fun UpdateAppointmentOutcomesDto.apply(projectCode: String) = updates.map { updateAppointmentOutcome ->
    val outcome = try {
      appointmentUpdateService.updateAppointmentOutcome(projectCode, updateAppointmentOutcome)
      UpdateAppointmentOutcomeResultType.SUCCESS
    } catch (_: NotFoundException) {
      UpdateAppointmentOutcomeResultType.NOT_FOUND
    } catch (_: ConflictException) {
      UpdateAppointmentOutcomeResultType.VERSION_CONFLICT
    } catch (t: Throwable) {
      sentryService.captureException(t)
      UpdateAppointmentOutcomeResultType.SERVER_ERROR
    }

    UpdateAppointmentOutcomeResultDto(updateAppointmentOutcome.deliusId, outcome)
  }
}
