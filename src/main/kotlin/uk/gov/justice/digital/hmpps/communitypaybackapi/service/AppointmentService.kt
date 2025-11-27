package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ConflictException
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.AppointmentMappers
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDomainEventDetail
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toUpdateAppointment
import java.util.UUID

@SuppressWarnings("LongParameterList")
@Service
class AppointmentService(
  private val appointmentOutcomeEntityRepository: AppointmentOutcomeEntityRepository,
  private val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
  private val appointmentMappers: AppointmentMappers,
  private val formService: FormService,
  private val appointmentOutcomeValidationService: AppointmentOutcomeValidationService,
  private val appointmentOutcomeEntityFactory: AppointmentOutcomeEntityFactory,
  private val contextService: ContextService,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun getAppointment(
    projectCode: String,
    appointmentId: Long,
  ): AppointmentDto = try {
    communityPaybackAndDeliusClient.getAppointment(
      projectCode = projectCode,
      appointmentId = appointmentId,
      username = contextService.getUserName(),
    ).let { appointmentMappers.toDto(it) }
  } catch (_: WebClientResponseException.NotFound) {
    throw NotFoundException("Appointment", appointmentId.toString())
  }

  fun getOutcomeDomainEventDetails(id: UUID) = appointmentOutcomeEntityRepository.findByIdOrNullForDomainEventDetails(id)?.toDomainEventDetail()

  @Transactional
  fun updateAppointmentOutcome(
    projectCode: String,
    outcome: UpdateAppointmentOutcomeDto,
  ) {
    val deliusId = outcome.deliusId

    appointmentOutcomeValidationService.validate(outcome)

    val proposedEntity = appointmentOutcomeEntityFactory.toEntity(outcome)

    appointmentOutcomeEntityRepository.findTopByAppointmentDeliusIdOrderByCreatedAtDesc(deliusId)?.let {
      if (it.isLogicallyIdentical(proposedEntity)) {
        log.debug("Not applying update for appointment $deliusId because the most recent update is logically identical")
        return
      }
    }

    val persistedEntity = appointmentOutcomeEntityRepository.save(proposedEntity)

    try {
      communityPaybackAndDeliusClient.updateAppointment(
        projectCode = projectCode,
        appointmentId = deliusId,
        updateAppointment = persistedEntity.toUpdateAppointment(),
      )
    } catch (_: WebClientResponseException.NotFound) {
      throw NotFoundException("Appointment", deliusId.toString())
    } catch (_: WebClientResponseException.Conflict) {
      throw ConflictException("A newer version of the appointment exists. Stale version is '${outcome.deliusVersionToUpdate}'")
    }

    outcome.formKeyToDelete?.let {
      formService.deleteIfExists(it)
    }
  }
}
