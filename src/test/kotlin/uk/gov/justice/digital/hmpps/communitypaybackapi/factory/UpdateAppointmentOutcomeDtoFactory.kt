package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AttendanceDataDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.EnforcementDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import java.time.LocalTime
import java.util.UUID

fun UpdateAppointmentOutcomeDto.Companion.valid(
  contactOutcomeId: UUID = UUID.randomUUID(),
  enforcementActionId: UUID = UUID.randomUUID(),
) = UpdateAppointmentOutcomeDto(
  deliusId = Long.random(),
  deliusVersionToUpdate = UUID.randomUUID(),
  startTime = LocalTime.of(10, 0),
  endTime = LocalTime.of(16, 0),
  contactOutcomeId = contactOutcomeId,
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
