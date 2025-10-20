package uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.service

import jakarta.transaction.Transactional
import org.apache.commons.lang3.builder.CompareToBuilder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.AppointmentOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.AppointmentOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.Behaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.WorkQuality
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.dto.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.AdditionalInformationType
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.DomainEventService
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.DomainEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.FormService
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.OffenderService
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.PersonReferenceType
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
        val offenderInfoResult = offenderService.getOffenderInfo(projectAppointment.case.crn)
        projectAppointment.toDto(offenderInfoResult)
      }
  } catch (_: WebClientResponseException.NotFound) {
    throw NotFoundException("Appointment", id.toString())
  }

  fun getOutcomeDomainEventDetails(id: UUID) = appointmentOutcomeEntityRepository.findByIdOrNullForDomainEventDetails(id)?.toDomainEventDetail()

  // DA: we should validate presence of attendanceData and enforcementData against contact outcome, once we have the reference data to do this
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

    val mostRecentAppointmentOutcome = appointmentOutcomeEntityRepository.findTopByAppointmentDeliusIdOrderByUpdatedAtDesc(deliusId)

    if (mostRecentAppointmentOutcome.isLogicallyIdentical(proposedEntity)) {
      log.debug("Not persisting update for appointment $deliusId because existing logically identical entry exists")
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
