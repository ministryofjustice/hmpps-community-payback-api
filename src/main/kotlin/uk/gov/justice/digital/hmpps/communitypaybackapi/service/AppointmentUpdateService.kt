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
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDomainEventDetail
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toUpdateAppointment
import java.util.UUID

// This is an orchestration service so the number of dependencies is acceptable
@SuppressWarnings("LongParameterList")
@Service
class AppointmentUpdateService(
  private val appointmentRetrievalService: AppointmentRetrievalService,
  private val appointmentOutcomeEntityRepository: AppointmentOutcomeEntityRepository,
  private val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
  private val formService: FormService,
  private val appointmentOutcomeValidationService: AppointmentOutcomeValidationService,
  private val appointmentOutcomeEntityFactory: AppointmentOutcomeEntityFactory,
  private val domainEventService: DomainEventService,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun getOutcomeDomainEventDetails(id: UUID) = appointmentOutcomeEntityRepository.findByIdOrNullForDomainEventDetails(id)?.toDomainEventDetail()

  @Transactional
  fun updateAppointmentOutcome(
    projectCode: String,
    update: UpdateAppointmentOutcomeDto,
  ) {
    val existingAppointment = appointmentRetrievalService.getAppointment(projectCode, update.deliusId)

    appointmentOutcomeValidationService.ensureUpdateIsValid(existingAppointment, update)

    val proposedEntity = appointmentOutcomeEntityFactory.toEntity(update, existingAppointment)

    if (hasUpdateAlreadyBeenSent(proposedEntity)) {
      log.debug("Not applying update for appointment ${update.deliusId} because the most recent update is logically identical")
      return
    }

    val persistedEntity = appointmentOutcomeEntityRepository.save(proposedEntity)

    domainEventService.publishOnTransactionCommit(
      id = persistedEntity.id,
      type = DomainEventType.APPOINTMENT_OUTCOME,
      additionalInformation = mapOf(AdditionalInformationType.APPOINTMENT_ID to update.deliusId),
      personReferences = mapOf(PersonReferenceType.CRN to existingAppointment.offender.crn),
    )

    updateDelius(projectCode, persistedEntity)

    update.formKeyToDelete?.let {
      formService.deleteIfExists(it)
    }
  }

  private fun hasUpdateAlreadyBeenSent(proposedEntity: AppointmentOutcomeEntity) = appointmentOutcomeEntityRepository
    .findTopByAppointmentDeliusIdOrderByCreatedAtDesc(proposedEntity.appointmentDeliusId)
    ?.isLogicallyIdentical(proposedEntity)
    ?: false

  @SuppressWarnings("SwallowedException", "ThrowsCount")
  private fun updateDelius(
    projectCode: String,
    outcomeEntity: AppointmentOutcomeEntity,
  ) {
    try {
      communityPaybackAndDeliusClient.updateAppointment(
        projectCode = projectCode,
        appointmentId = outcomeEntity.appointmentDeliusId,
        updateAppointment = outcomeEntity.toUpdateAppointment(),
      )
    } catch (_: WebClientResponseException.NotFound) {
      throw NotFoundException("Appointment", outcomeEntity.appointmentDeliusId.toString())
    } catch (_: WebClientResponseException.Conflict) {
      throw ConflictException("A newer version of the appointment exists. Stale version is '${outcomeEntity.deliusVersionToUpdate}'")
    } catch (badRequest: WebClientResponseException.BadRequest) {
      throw InternalServerErrorException("Bad request returned updating an appointment. Upstream response is '${badRequest.responseBodyAsString}'")
    }
  }
}
