package uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.dto.OffenderDto
import java.time.LocalDate
import java.time.LocalTime

data class AppointmentDto(
  // If we are going to use this DTO for draft updates then we are missing some fields.
  // crn comes from offender dto
  // supervising team vs supervising team code
  // date = appointment date
  // contact outcome
  // delius last updated date
  val id: Long, // = delius app id
  val projectName: String,
  val projectCode: String,
  val projectTypeName: String,
  val projectTypeCode: String,
  val offender: OffenderDto,
  val supervisingTeam: String,
  val date: LocalDate,
  val startTime: LocalTime,
  val endTime: LocalTime,
  val attendanceData: AttendanceDataDto?,
  val enforcementData: EnforcementDto?,
  val notes: String?,
)
