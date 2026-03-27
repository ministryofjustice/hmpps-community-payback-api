package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.ConflictException
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.InternalServerErrorException
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentEventEntityFactory.UpdateAppointmentEventDetails
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentValidationService.ValidatedAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toNDUpdateAppointment

@Service
class AppointmentUpdateService(
  private val appointmentRetrievalService: AppointmentRetrievalService,
  private val appointmentEventService: AppointmentEventService,
  private val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
  private val appointmentUpdateValidationService: AppointmentValidationService,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun updateAppointmentOutcome(
    projectCode: String,
    update: UpdateAppointmentOutcomeDto,
    trigger: AppointmentEventTrigger,
  ) {
    val existingAppointment = appointmentRetrievalService.getAppointment(projectCode, update.deliusId)
    val appointmentEntity = appointmentRetrievalService.getOrCreateAppointmentEntity(existingAppointment)

    val validatedUpdateDto = appointmentUpdateValidationService.validateUpdate(existingAppointment, update)

    val updateEventDetails = UpdateAppointmentEventDetails(
      validatedUpdate = appointmentUpdateValidationService.validateUpdate(existingAppointment, update),
      appointment = appointmentEntity,
      existingAppointment = existingAppointment,
      trigger = trigger,
    )

    if (appointmentEventService.hasUpdateAlreadyBeenSent(updateEventDetails)) {
      log.debug("Not applying update for appointment ${update.deliusId} because the most recent update is logically identical")
      return
    }

    updateDelius(existingAppointment, validatedUpdateDto)

    appointmentEventService.publishUpdateEventOnTransactionCommit(updateEventDetails)
  }

  @SuppressWarnings("SwallowedException", "ThrowsCount")
  private fun updateDelius(
    existingAppointment: AppointmentDto,
    validatedUpdateDto: ValidatedAppointment<UpdateAppointmentOutcomeDto>,
  ) {
    val deliusAppointmentId = validatedUpdateDto.dto.deliusId
    try {
      communityPaybackAndDeliusClient.updateAppointment(
        projectCode = existingAppointment.projectCode,
        appointmentId = deliusAppointmentId,
        updateAppointment = validatedUpdateDto.toNDUpdateAppointment(existingAppointment),
      )
    } catch (_: WebClientResponseException.NotFound) {
      throw NotFoundException("Appointment", deliusAppointmentId.toString())
    } catch (_: WebClientResponseException.Conflict) {
      throw ConflictException("A newer version of the appointment exists. Stale version is '${validatedUpdateDto.dto.deliusVersionToUpdate}'")
    } catch (badRequest: WebClientResponseException.BadRequest) {
      throw InternalServerErrorException("Bad request returned updating an appointment. Upstream response is '${badRequest.responseBodyAsString}'")
    }
  }
}
