package uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.dto.OffenderDto
import java.time.LocalDate
import java.time.LocalTime

data class AppointmentDto(
  val id: Long,
  val projectName: String,
  val projectCode: String,
  val offender: OffenderDto,
  val supervisingTeam: String,
  val date: LocalDate,
  val startTime: LocalTime,
  val endTime: LocalTime,
  val attendanceData: AttendanceDataDto?,
  val enforcementData: EnforcementDto?,
  val notes: String?,
)
