package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.AppointmentBehaviourDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.AppointmentWorkQualityDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.AttendanceDataDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.EnforcementDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.UpdateAppointmentOutcomesDto
import java.util.UUID

fun UpdateAppointmentOutcomesDto.Companion.valid(
  vararg ids: Long = longArrayOf(Long.random()),
  contactOutcomeId: UUID = UUID.randomUUID(),
  enforcementActionId: UUID = UUID.randomUUID(),
) = UpdateAppointmentOutcomesDto(
  ids = ids.toList(),
  outcomeData = UpdateAppointmentOutcomeDto.valid(
    contactOutcomeId = contactOutcomeId,
    enforcementActionId = enforcementActionId,
  ),
)

fun UpdateAppointmentOutcomeDto.Companion.valid(contactOutcomeId: UUID, enforcementActionId: UUID) = UpdateAppointmentOutcomeDto(
  startTime = randomLocalTime(),
  endTime = randomLocalTime(),
  contactOutcomeId = contactOutcomeId,
  supervisorOfficerCode = String.random(),
  notes = String.random(400),
  attendanceData = AttendanceDataDto(
    hiVisWorn = Boolean.random(),
    workedIntensively = Boolean.random(),
    penaltyMinutes = Long.random(1, 100),
    workQuality = AppointmentWorkQualityDto.entries.toTypedArray().random(),
    behaviour = AppointmentBehaviourDto.entries.toTypedArray().random(),
  ),
  enforcementData = EnforcementDto(
    enforcementActionId = enforcementActionId,
    respondBy = randomLocalDate(),
  ),
)
