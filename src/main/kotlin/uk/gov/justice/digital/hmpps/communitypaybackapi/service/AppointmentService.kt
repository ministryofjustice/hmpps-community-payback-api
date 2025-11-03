package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import jakarta.transaction.Transactional
import org.apache.commons.lang3.builder.CompareToBuilder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ConflictException
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.AppointmentMappers
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDomainEventDetail
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toUpdateAppointment
import java.util.UUID

@Service
class AppointmentService(
  private val appointmentOutcomeEntityRepository: AppointmentOutcomeEntityRepository,
  private val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
  private val appointmentMappers: AppointmentMappers,
  private val formService: FormService,
  private val appointmentOutcomeValidationService: AppointmentOutcomeValidationService,
  private val appointmentOutcomeEntityFactory: AppointmentOutcomeEntityFactory,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun getAppointment(id: Long): AppointmentDto = try {
    communityPaybackAndDeliusClient.getAppointment(id).let { appointmentMappers.toDto(it) }
  } catch (_: WebClientResponseException.NotFound) {
    throw NotFoundException("Appointment", id.toString())
  }

  fun getOutcomeDomainEventDetails(id: UUID) = appointmentOutcomeEntityRepository.findByIdOrNullForDomainEventDetails(id)?.toDomainEventDetail()

  @Transactional
  fun updateAppointmentOutcome(
    outcome: UpdateAppointmentOutcomeDto,
  ) {
    val deliusId = outcome.deliusId

    appointmentOutcomeValidationService.validate(outcome)

    val proposedEntity = appointmentOutcomeEntityFactory.toEntity(outcome)

    val mostRecentAppointmentOutcome = appointmentOutcomeEntityRepository.findTopByAppointmentDeliusIdOrderByCreatedAtDesc(deliusId)

    if (mostRecentAppointmentOutcome.isLogicallyIdentical(proposedEntity)) {
      log.debug("Not applying update for appointment $deliusId because the most recent update is logically identical")
      return
    }

    val persistedEntity = appointmentOutcomeEntityRepository.save(proposedEntity)

    try {
      communityPaybackAndDeliusClient.updateAppointment(
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

  private fun AppointmentOutcomeEntity?.isLogicallyIdentical(other: AppointmentOutcomeEntity) = this != null &&
    CompareToBuilder.reflectionCompare(
      this,
      other,
      "id",
      "createdAt",
      "updatedAt",
    ) == 0
}
