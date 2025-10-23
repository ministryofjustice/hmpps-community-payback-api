package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import jakarta.transaction.Transactional
import org.apache.commons.lang3.builder.CompareToBuilder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.Behaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.WorkQuality
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.fromDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDomainEventDetail
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto
import java.util.UUID

@Service
class AppointmentService(
  private val appointmentOutcomeEntityRepository: AppointmentOutcomeEntityRepository,
  private val domainEventService: DomainEventService,
  private val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
  private val offenderService: OffenderService,
  private val formService: FormService,
  private val appointmentOutcomeValidationService: AppointmentOutcomeValidationService,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    const val SECONDS_PER_MINUTE = 60L
  }

  fun getAppointment(id: Long): AppointmentDto = try {
    communityPaybackAndDeliusClient.getProjectAppointment(id)
      .let { projectAppointment ->
        val offenderInfoResult = offenderService.toOffenderInfo(projectAppointment.case)
        projectAppointment.toDto(offenderInfoResult)
      }
  } catch (_: WebClientResponseException.NotFound) {
    throw NotFoundException("Appointment", id.toString())
  }

  fun getOutcomeDomainEventDetails(id: UUID) = appointmentOutcomeEntityRepository.findByIdOrNullForDomainEventDetails(id)?.toDomainEventDetail()

  @Transactional
  fun updateAppointmentOutcome(
    deliusId: Long,
    outcome: UpdateAppointmentOutcomeDto,
  ) {
    val crn = try {
      communityPaybackAndDeliusClient.getProjectAppointment(deliusId).case.crn
    } catch (_: WebClientResponseException.NotFound) {
      throw NotFoundException("Appointment", deliusId.toString())
    }

    appointmentOutcomeValidationService.validate(outcome)

    val proposedEntity = toEntity(deliusId, outcome)

    val mostRecentAppointmentOutcome = appointmentOutcomeEntityRepository.findTopByAppointmentDeliusIdOrderByCreatedAtDesc(deliusId)

    if (mostRecentAppointmentOutcome.isLogicallyIdentical(proposedEntity)) {
      log.debug("Not applying update for appointment $deliusId because the most recent update is logically identical")
      return
    }

    val persistedEntity = appointmentOutcomeEntityRepository.save(proposedEntity)

    domainEventService.publish(
      id = persistedEntity.id,
      type = DomainEventType.APPOINTMENT_OUTCOME,
      additionalInformation = mapOf(AdditionalInformationType.APPOINTMENT_ID to deliusId),
      personReferences = mapOf(PersonReferenceType.CRN to crn),
    )

    outcome.formKeyToDelete?.let {
      formService.deleteIfExists(it)
    }
  }

  fun toEntity(deliusId: Long, outcome: UpdateAppointmentOutcomeDto) = AppointmentOutcomeEntity(
    id = UUID.randomUUID(),
    appointmentDeliusId = deliusId,
    deliusVersionToUpdate = outcome.deliusVersionToUpdate,
    startTime = outcome.startTime,
    endTime = outcome.endTime,
    contactOutcomeId = outcome.contactOutcomeId,
    enforcementActionId = outcome.enforcementData?.enforcementActionId,
    supervisorOfficerCode = outcome.supervisorOfficerCode,
    notes = outcome.notes,
    hiVisWorn = outcome.attendanceData?.hiVisWorn,
    workedIntensively = outcome.attendanceData?.workedIntensively,
    penaltyMinutes = outcome.attendanceData?.penaltyTime?.toSecondOfDay()?.div(SECONDS_PER_MINUTE),
    workQuality = outcome.attendanceData?.workQuality?.let { WorkQuality.fromDto(it) },
    behaviour = outcome.attendanceData?.behaviour?.let { Behaviour.fromDto(it) },
    respondBy = outcome.enforcementData?.respondBy,
    alertActive = outcome.alertActive,
    sensitive = outcome.sensitive,
  )

  private fun AppointmentOutcomeEntity?.isLogicallyIdentical(other: AppointmentOutcomeEntity) = this != null &&
    CompareToBuilder.reflectionCompare(
      this,
      other,
      "id",
      "createdAt",
      "updatedAt",
    ) == 0
}
