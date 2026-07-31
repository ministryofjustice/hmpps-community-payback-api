package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestContextHolder
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.runWithCallerContext
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
  private companion object {
    const val MAX_CONCURRENT_UPDATES = 4
  }

  private val log = LoggerFactory.getLogger(this::class.java)

  fun updateAppointments(
    projectCode: String,
    request: UpdateAppointmentOutcomesDto,
    trigger: AppointmentEventTrigger,
  ): UpdateAppointmentsOutcomesResultDto = runBlocking {
    val dispatcher = Dispatchers.IO.limitedParallelism(MAX_CONCURRENT_UPDATES)
    val securityContext = SecurityContextHolder.getContext()
    val requestAttributes = RequestContextHolder.getRequestAttributes()
    val results = request.updates.map { update ->
      async(dispatcher) {
        runWithCallerContext(securityContext, requestAttributes) {
          updateAppointment(projectCode, update, trigger)
        }
      }
    }.awaitAll()

    UpdateAppointmentsOutcomesResultDto(results)
  }

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
      val validatedUpdate = appointmentUpdateValidationService.validateUpdate(existingAppointment, update)
      appointmentUpdateService.updateAppointment(
        existingAppointment = existingAppointment,
        validatedUpdate = validatedUpdate,
        trigger = trigger,
      )
      result(id, UpdateAppointmentOutcomeResultType.SUCCESS)
    } catch (e: BadRequestException) {
      logUpdateException(id, e)
      result(id, UpdateAppointmentOutcomeResultType.VALIDATION_ERROR, e.message)
    } catch (e: ConflictException) {
      logUpdateException(id, e)
      result(id, UpdateAppointmentOutcomeResultType.VERSION_CONFLICT)
    } catch (t: Throwable) {
      logUpdateException(id, t)
      sentryService.captureException(t)
      result(id, UpdateAppointmentOutcomeResultType.SERVER_ERROR, t.message)
    }
  }

  private fun logUpdateException(id: DeliusAppointmentIdDto, exception: Throwable) {
    log.info(
      "Bulk update failed for project code {} and Delius appointment ID {} with {}: {}",
      id.projectCode,
      id.deliusAppointmentId,
      exception.javaClass.simpleName,
      exception.message,
    )
  }

  private fun result(
    id: DeliusAppointmentIdDto,
    type: UpdateAppointmentOutcomeResultType,
    message: String? = null,
  ) = UpdateAppointmentOutcomeResultDto(id.deliusAppointmentId, type, message)
}
