package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AttendanceDataDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAppointmentDto
import java.time.LocalTime
import java.util.UUID

fun CreateAppointmentDto.Companion.valid() = CreateAppointmentDto(
  id = UUID.randomUUID(),
  crn = String.random(5),
  deliusEventNumber = Int.random(50),
  allocationId = Long.random(),
  projectCode = String.random(5),
  date = randomLocalDate(),
  startTime = LocalTime.of(10, 0),
  endTime = LocalTime.of(16, 0),
  pickUpLocationCode = String.random(5),
  pickUpLocationDescription = String.random(50),
  pickUpTime = randomLocalTime(),
  contactOutcomeCode = String.random(5),
  supervisorOfficerCode = String.random(),
  notes = String.random(400),
  attendanceData = AttendanceDataDto.valid(),
  alertActive = Boolean.random(),
  sensitive = Boolean.random(),
)
