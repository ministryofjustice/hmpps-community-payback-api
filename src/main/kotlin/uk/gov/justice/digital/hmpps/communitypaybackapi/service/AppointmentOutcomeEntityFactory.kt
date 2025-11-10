package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.Behaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EnforcementActionEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.WorkQuality
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.fromDto
import java.util.UUID

@Service
class AppointmentOutcomeEntityFactory(
  private val contactOutcomeEntityRepository: ContactOutcomeEntityRepository,
  private val enforcementActionEntityRepository: EnforcementActionEntityRepository,
) {

  private companion object {
    const val SECONDS_PER_MINUTE = 60L
  }

  fun toEntity(
    outcome: UpdateAppointmentOutcomeDto,
  ) = AppointmentOutcomeEntity(
    id = UUID.randomUUID(),
    appointmentDeliusId = outcome.deliusId,
    deliusVersionToUpdate = outcome.deliusVersionToUpdate,
    startTime = outcome.startTime,
    endTime = outcome.endTime,
    contactOutcome = contactOutcomeEntityRepository.findByIdOrNull(outcome.contactOutcomeId)!!,
    enforcementAction = outcome.enforcementData?.enforcementActionId?.let { enforcementActionEntityRepository.findByIdOrNull(it)!! },
    supervisorOfficerCode = outcome.supervisorOfficerCode,
    notes = outcome.notes,
    hiVisWorn = outcome.attendanceData?.hiVisWorn,
    workedIntensively = outcome.attendanceData?.workedIntensively,
    penaltyMinutes = outcome.attendanceData?.penaltyTime?.duration?.toMinutes(),
    workQuality = outcome.attendanceData?.workQuality?.let { WorkQuality.fromDto(it) },
    behaviour = outcome.attendanceData?.behaviour?.let { Behaviour.fromDto(it) },
    respondBy = outcome.enforcementData?.respondBy,
    alertActive = outcome.alertActive,
    sensitive = outcome.sensitive,
  )
}
