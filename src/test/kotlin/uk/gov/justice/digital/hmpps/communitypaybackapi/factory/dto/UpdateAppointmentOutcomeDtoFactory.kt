package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto

import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AttendanceDataDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

fun UpdateAppointmentOutcomeDto.Companion.valid(
  contactOutcomeCode: String = "OUTCOME1",
) = UpdateAppointmentOutcomeDto(
  deliusId = Long.random(),
  deliusVersionToUpdate = UUID.randomUUID(),
  date = LocalDate.now(),
  startTime = LocalTime.of(10, 0),
  endTime = LocalTime.of(16, 0),
  contactOutcomeCode = contactOutcomeCode,
  supervisorOfficerCode = String.random(),
  notes = String.random(400),
  attendanceData = AttendanceDataDto.valid(),
  alertActive = Boolean.random(),
  sensitive = Boolean.random(),
)
