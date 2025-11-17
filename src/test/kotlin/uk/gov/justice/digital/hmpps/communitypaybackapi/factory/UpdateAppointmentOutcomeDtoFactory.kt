package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AttendanceDataDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.EnforcementDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import java.time.LocalTime

fun UpdateAppointmentOutcomeDto.Companion.valid(
  contactOutcomeCode: String = "OUTCOME1",
  enforcementActionId: java.util.UUID = java.util.UUID.randomUUID(),
) = UpdateAppointmentOutcomeDto(
  deliusId = Long.random(),
  deliusVersionToUpdate = java.util.UUID.randomUUID(),
  startTime = LocalTime.of(10, 0),
  endTime = LocalTime.of(16, 0),
  contactOutcomeCode = contactOutcomeCode,
  supervisorOfficerCode = String.random(),
  notes = String.random(400),
  attendanceData = AttendanceDataDto.valid(),
  enforcementData = EnforcementDto(
    enforcementActionId = enforcementActionId,
    respondBy = randomLocalDate(),
  ),
  formKeyToDelete = null,
  alertActive = Boolean.random(),
  sensitive = Boolean.random(),
)
