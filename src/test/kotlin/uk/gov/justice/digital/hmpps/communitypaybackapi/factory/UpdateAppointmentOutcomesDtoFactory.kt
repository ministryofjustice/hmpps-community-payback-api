package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.AppointmentBehaviourDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.AppointmentWorkQualityDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.UpdateAppointmentAttendanceDataDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.UpdateAppointmentEnforcementDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.UpdateAppointmentOutcomesDto

fun UpdateAppointmentOutcomesDto.Companion.valid(
  vararg ids: Long = longArrayOf(Long.random()),
) = UpdateAppointmentOutcomesDto(
  ids = ids.toList(),
  outcomeData = UpdateAppointmentOutcomeDto.valid(),
)

fun UpdateAppointmentOutcomeDto.Companion.valid() = UpdateAppointmentOutcomeDto(
  projectTypeId = Long.random(),
  startTime = randomLocalTime(),
  endTime = randomLocalTime(),
  contactOutcomeId = Long.random(),
  supervisorTeamId = Long.random(),
  supervisorOfficerId = Long.random(),
  notes = String.random(400),
  attendanceData = UpdateAppointmentAttendanceDataDto(
    hiVisWarn = Boolean.random(),
    workedIntensively = Boolean.random(),
    penaltyMinutes = Long.random(0, 600),
    workQuality = AppointmentWorkQualityDto.entries.toTypedArray().random(),
    behaviour = AppointmentBehaviourDto.entries.toTypedArray().random(),
  ),
  enforcementData = UpdateAppointmentEnforcementDto(
    enforcementActionId = Long.random(),
    respondBy = randomLocalDate(),
  ),
)
