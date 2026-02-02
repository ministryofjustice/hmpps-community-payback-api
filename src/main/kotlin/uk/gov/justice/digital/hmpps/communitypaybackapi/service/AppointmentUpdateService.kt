package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.ConflictException
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.InternalServerErrorException
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toNDUpdateAppointment

@Service
class AppointmentUpdateService(
  private val appointmentRetrievalService: AppointmentRetrievalService,
  private val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
  private val formService: FormService,
  private val appointmentOutcomeValidationService: AppointmentUpdateValidationService,
  private val appointmentEventService: AppointmentEventService,
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

    appointmentOutcomeValidationService.ensureUpdateIsValid(existingAppointment, update)

    val createEventOutcome = appointmentEventService.createUpdateEvent(
      update,
      trigger,
      existingAppointment,
    )

    when (createEventOutcome) {
      AppointmentEventService.EventCreationOutcome.DuplicateIgnored -> {
        log.debug("Not applying update for appointment ${update.deliusId} because the most recent update is logically identical")
        return
      }
      is AppointmentEventService.EventCreationOutcome.EventCreated -> {
        updateDelius(projectCode, createEventOutcome.event)
      }
    }

    update.formKeyToDelete?.let {
      formService.deleteIfExists(it)
    }
  }

  @SuppressWarnings("SwallowedException", "ThrowsCount")
  private fun updateDelius(
    projectCode: String,
    appointmentEvent: AppointmentEventEntity,
  ) {
    try {
      communityPaybackAndDeliusClient.updateAppointment(
        projectCode = projectCode,
        appointmentId = appointmentEvent.deliusAppointmentId,
        updateAppointment = appointmentEvent.toNDUpdateAppointment(),
      )
    } catch (_: WebClientResponseException.NotFound) {
      throw NotFoundException("Appointment", appointmentEvent.deliusAppointmentId.toString())
    } catch (_: WebClientResponseException.Conflict) {
      throw ConflictException("A newer version of the appointment exists. Stale version is '${appointmentEvent.priorDeliusVersion}'")
    } catch (badRequest: WebClientResponseException.BadRequest) {
      throw InternalServerErrorException("Bad request returned updating an appointment. Upstream response is '${badRequest.responseBodyAsString}'")
    }
  }
}
