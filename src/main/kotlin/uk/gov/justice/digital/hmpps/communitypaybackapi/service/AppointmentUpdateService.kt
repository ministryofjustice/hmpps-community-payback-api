package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.ConflictException
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.InternalServerErrorException
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.AppointmentUpdatedEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.SpringEventPublisher
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toNDUpdateAppointment

@Service
class AppointmentUpdateService(
  private val appointmentRetrievalService: AppointmentRetrievalService,
  private val appointmentEventService: AppointmentEventService,
  private val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
  private val updateAppointmentValidationService: UpdateAppointmentValidationService,
  private val springEventPublisher: SpringEventPublisher,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun updateAppointment(
    existingAppointment: AppointmentDto,
    update: UpdateAppointmentDto,
    trigger: AppointmentEventTrigger,
  ) {
    val validatedUpdate = getValidatedUpdate(existingAppointment, update)

    val appointmentEntity = appointmentRetrievalService.getOrCreateAppointmentEntity(existingAppointment)

    val updateEventDetails = AppointmentUpdatedEvent(
      updateDto = validatedUpdate,
      appointmentEntity = appointmentEntity,
      existingAppointment = existingAppointment,
      trigger = trigger,
    )

    if (appointmentEventService.hasUpdateAlreadyBeenSent(updateEventDetails)) {
      log.debug("Not applying update for appointment ${validatedUpdate.dto.deliusId} because the most recent update is logically identical")
      return
    }

    updateDelius(existingAppointment, validatedUpdate)

    springEventPublisher.publishEvent(updateEventDetails)
  }

  private fun getValidatedUpdate(
    existingAppointment: AppointmentDto,
    update: UpdateAppointmentDto,
  ): ValidatedAppointment<UpdateAppointmentDto> {
    val ctx = AppointmentValidationService.AppointmentValidationContext.Update(existingAppointment)

    val validationResult = updateAppointmentValidationService.validate(update, ctx)

    if (validationResult.hasErrors) {
      throwValidationErrorForAppointmentUpdateCreate(validationResult.errors[0])
    }

    return ValidatedAppointment(
      dto = update,
      minutesToCredit = ctx.timeToCredit,
      contactOutcome = ctx.contactOutcome.value,
      pickUpLocation = ctx.pickUpLocation.value,
      project = ctx.project!!,
    )
  }

  @SuppressWarnings("SwallowedException", "ThrowsCount")
  private fun updateDelius(
    existingAppointment: AppointmentDto,
    validatedUpdateDto: ValidatedAppointment<UpdateAppointmentDto>,
  ) {
    val deliusAppointmentId = validatedUpdateDto.dto.deliusId
    try {
      communityPaybackAndDeliusClient.updateAppointment(
        projectCode = existingAppointment.projectCode,
        appointmentId = deliusAppointmentId,
        updateAppointment = validatedUpdateDto.toNDUpdateAppointment(existingAppointment),
      )
    } catch (_: WebClientResponseException.Conflict) {
      throw ConflictException("A newer version of the appointment exists. Stale version is '${validatedUpdateDto.dto.deliusVersionToUpdate}'")
    } catch (badRequest: WebClientResponseException.BadRequest) {
      throw InternalServerErrorException("Bad request returned updating an appointment. Upstream response is '${badRequest.responseBodyAsString}'")
    }
  }
}
