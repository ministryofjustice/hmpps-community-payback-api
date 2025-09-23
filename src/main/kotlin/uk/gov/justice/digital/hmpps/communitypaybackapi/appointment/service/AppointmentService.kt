package uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.service

import jakarta.transaction.Transactional
import org.apache.commons.lang3.builder.CompareToBuilder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.UpdateAppointmentOutcomesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.AppointmentOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.AppointmentOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.Behaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.WorkQuality
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.AdditionalInformationType
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.DomainEventService
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.DomainEventType
import java.util.UUID

@Service
class AppointmentService(
  val appointmentOutcomeEntityRepository: AppointmentOutcomeEntityRepository,
  val domainEventService: DomainEventService,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  // DA: we should validate presence of attendanceData and enforcementData against contact outcome
  // DA: will need to return to this once we started linking to local entities, potentially returning 400 if invalid ids provided
  // DA: do we want to validate ids against ref data we pull from upstream APIs?
  @Transactional
  fun updateAppointmentsOutcome(updateAppointments: UpdateAppointmentOutcomesDto) {
    updateAppointments.ids.forEach { updateAppointmentsOutcome(it, updateAppointments.outcomeData) }
  }

  private fun updateAppointmentsOutcome(
    deliusId: Long,
    outcome: UpdateAppointmentOutcomeDto,
  ) {
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
    )
  }

  fun toEntity(deliusId: Long, outcome: UpdateAppointmentOutcomeDto) = AppointmentOutcomeEntity(
    id = UUID.randomUUID(),
    appointmentDeliusId = deliusId,
    projectTypeDeliusId = outcome.projectTypeId,
    startTime = outcome.startTime,
    endTime = outcome.endTime,
    contactOutcomeId = outcome.contactOutcomeId,
    enforcementActionId = outcome.enforcementData?.enforcementActionId,
    // DA: team is redundant?
    supervisorTeamDeliusId = outcome.supervisorTeamId,
    supervisorOfficerDeliusId = outcome.supervisorOfficerId,
    notes = outcome.notes,
    hiVisWorn = outcome.attendanceData?.hiVisWarn,
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
}
