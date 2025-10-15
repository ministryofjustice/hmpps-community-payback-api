package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.AppointmentBehaviourDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.AppointmentWorkQualityDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.AttendanceDataDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.EnforcementDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.UpdateAppointmentOutcomeDto
import java.util.UUID

fun UpdateAppointmentOutcomeDto.Companion.valid(
  contactOutcomeId: UUID = UUID.randomUUID(),
  enforcementActionId: UUID = UUID.randomUUID(),
) = UpdateAppointmentOutcomeDto(
  startTime = randomLocalTime(),
  endTime = randomLocalTime(),
  contactOutcomeId = contactOutcomeId,
  supervisorOfficerCode = String.random(),
  notes = String.random(400),
  attendanceData = AttendanceDataDto(
    hiVisWorn = Boolean.random(),
    workedIntensively = Boolean.random(),
    penaltyTime = randomLocalTime(),
    workQuality = AppointmentWorkQualityDto.entries.toTypedArray().random(),
    behaviour = AppointmentBehaviourDto.entries.toTypedArray().random(),
  ),
  enforcementData = EnforcementDto(
    enforcementActionId = enforcementActionId,
    respondBy = randomLocalDate(),
  ),
  formKeyToDelete = null,
)
