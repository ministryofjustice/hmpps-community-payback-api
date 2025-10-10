package uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.service

import jakarta.transaction.Transactional
import org.apache.commons.lang3.builder.CompareToBuilder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.AppointmentDraftDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.UpdateAppointmentOutcomesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.UpsertAppointmentDraftDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.AppointmentDraftEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.AppointmentDraftEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.AppointmentOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.AppointmentOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.Behaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.WorkQuality
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.dto.BadRequestException
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.dto.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.AdditionalInformationType
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.DomainEventService
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.DomainEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.OffenderService
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.PersonReferenceType
import java.util.UUID

@Service
class AppointmentService(
  private val appointmentOutcomeEntityRepository: AppointmentOutcomeEntityRepository,
  private val domainEventService: DomainEventService,
  private val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
  private val appointmentDraftEntityRepository: AppointmentDraftEntityRepository,
  private val offenderService: OffenderService,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    const val SECONDS_PER_MINUTE = 60L
  }

  fun getAppointment(id: Long): AppointmentDto = try {
    communityPaybackAndDeliusClient.getProjectAppointment(id)
      .let { projectAppointment ->
        val offenderInfoResult = offenderService.getOffenderInfo(projectAppointment.crn)
        projectAppointment.toDto(offenderInfoResult)
      }
  } catch (_: WebClientResponseException.NotFound) {
    throw NotFoundException("Appointment", id.toString())
  }

  // DA: we should validate presence of attendanceData and enforcementData against contact outcome
  @Transactional
  fun updateAppointmentsOutcome(updateAppointments: UpdateAppointmentOutcomesDto) {
    updateAppointments.ids.forEach { updateAppointmentsOutcome(it, updateAppointments.outcomeData) }
  }

  fun getOutcomeDomainEventDetails(id: UUID) = appointmentOutcomeEntityRepository.findByIdOrNullForDomainEventDetails(id)?.toDomainEventDetail()

  private fun updateAppointmentsOutcome(
    deliusId: Long,
    outcome: UpdateAppointmentOutcomeDto,
  ) {
    val crn = try {
      communityPaybackAndDeliusClient.getProjectAppointment(deliusId).crn
    } catch (_: WebClientResponseException.NotFound) {
      throw BadRequestException("Appointment not found for ID '$deliusId'")
    }

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
    penaltyMinutes = outcome.attendanceData?.penaltyMinutes,
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

  @Transactional
  fun upsertAppointmentDraft(
    deliusAppointmentId: Long,
    request: UpsertAppointmentDraftDto,
  ): AppointmentDraftDto {
    val existing = appointmentDraftEntityRepository.findByAppointmentDeliusId(deliusAppointmentId)

    val entityToSave = existing?.updateFrom(request)
      ?: request.toEntity(deliusAppointmentId)

    return appointmentDraftEntityRepository.save(entityToSave).toDto()
  }

  private fun AppointmentDraftEntity.updateFrom(request: UpsertAppointmentDraftDto): AppointmentDraftEntity = copy(
    crn = request.crn,
    projectName = request.projectName,
    projectCode = request.projectCode,
    projectTypeId = request.projectTypeId,
    supervisingTeamCode = request.supervisingTeamCode,
    appointmentDate = request.appointmentDate,
    startTime = request.startTime,
    endTime = request.endTime,
    hiVisWorn = request.attendanceData?.hiVisWorn,
    workedIntensively = request.attendanceData?.workedIntensively,
    penaltyTimeMinutes = request.attendanceData?.penaltyMinutes,
    workQuality = request.attendanceData?.workQuality?.let { WorkQuality.fromDto(it) },
    behaviour = request.attendanceData?.behaviour?.let { Behaviour.fromDto(it) },
    supervisorOfficerCode = request.attendanceData?.supervisorOfficerCode,
    contactOutcomeId = request.attendanceData?.contactOutcomeId,
    enforcementActionId = request.enforcementData?.enforcementActionId,
    respondBy = request.enforcementData?.respondBy,
    notes = request.notes,
    deliusLastUpdatedAt = request.deliusLastUpdatedAt,
  )

  private fun UpsertAppointmentDraftDto.toEntity(deliusAppointmentId: Long): AppointmentDraftEntity = AppointmentDraftEntity(
    id = UUID.randomUUID(),
    appointmentDeliusId = deliusAppointmentId,
    crn = crn,
    projectName = projectName,
    projectCode = projectCode,
    projectTypeId = projectTypeId,
    supervisingTeamCode = supervisingTeamCode,
    appointmentDate = appointmentDate,
    startTime = startTime,
    endTime = endTime,
    hiVisWorn = attendanceData?.hiVisWorn,
    workedIntensively = attendanceData?.workedIntensively,
    penaltyTimeMinutes = attendanceData?.penaltyMinutes,
    workQuality = attendanceData?.workQuality?.let { WorkQuality.fromDto(it) },
    behaviour = attendanceData?.behaviour?.let { Behaviour.fromDto(it) },
    supervisorOfficerCode = attendanceData?.supervisorOfficerCode,
    contactOutcomeId = attendanceData?.contactOutcomeId,
    enforcementActionId = enforcementData?.enforcementActionId,
    respondBy = enforcementData?.respondBy,
    notes = notes,
    deliusLastUpdatedAt = deliusLastUpdatedAt,
  )
}
