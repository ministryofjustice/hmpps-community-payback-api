package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AttendanceDataDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.Behaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.WorkQuality
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.fromDto
import java.time.Duration
import java.time.LocalTime
import java.util.UUID

@Service
class AppointmentEventEntityFactory(
  private val contactOutcomeEntityRepository: ContactOutcomeEntityRepository,
) {

  fun toEntity(
    outcome: UpdateAppointmentOutcomeDto,
    existingAppointment: AppointmentDto,
  ): AppointmentEventEntity {
    val startTime = outcome.startTime
    val endTime = outcome.endTime
    val penaltyMinutes = outcome.attendanceData?.derivePenaltyMinutesDuration()?.toMinutes()
    val contactOutcome = outcome.contactOutcomeCode?.let {
      contactOutcomeEntityRepository.findByCode(it) ?: error("ContactOutcome not found for code: $it")
    }

    return AppointmentEventEntity(
      id = UUID.randomUUID(),
      crn = existingAppointment.offender.crn,
      appointmentDeliusId = outcome.deliusId,
      deliusVersionToUpdate = outcome.deliusVersionToUpdate,
      deliusEventNumber = existingAppointment.deliusEventNumber,
      startTime = startTime,
      endTime = endTime,
      contactOutcome = contactOutcome,
      supervisorOfficerCode = outcome.supervisorOfficerCode,
      notes = outcome.notes,
      hiVisWorn = outcome.attendanceData?.hiVisWorn,
      workedIntensively = outcome.attendanceData?.workedIntensively,
      penaltyMinutes = penaltyMinutes,
      minutesCredited = calculateMinutesCredited(
        startTime = startTime,
        endTime = endTime,
        penaltyMinutes = penaltyMinutes,
        contactOutcome = contactOutcome,
      ),
      workQuality = outcome.attendanceData?.workQuality?.let { WorkQuality.fromDto(it) },
      behaviour = outcome.attendanceData?.behaviour?.let { Behaviour.fromDto(it) },
      alertActive = outcome.alertActive,
      sensitive = outcome.sensitive,
    )
  }

  private fun calculateMinutesCredited(
    startTime: LocalTime,
    endTime: LocalTime,
    penaltyMinutes: Long?,
    contactOutcome: ContactOutcomeEntity?,
  ): Long? {
    if (contactOutcome?.attended != true) return null

    val minutesCredited = Duration.between(startTime, endTime).toMinutes() - (penaltyMinutes ?: 0L)
    return minutesCredited.takeIf { it != 0L }
  }
}

fun AttendanceDataDto.derivePenaltyMinutesDuration() = penaltyMinutes?.let { Duration.ofMinutes(it) } ?: penaltyTime?.duration
