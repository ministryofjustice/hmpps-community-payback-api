package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import java.time.LocalDate
import java.time.LocalTime

interface AppointmentCommandDto {
  val startTime: LocalTime
  val endTime: LocalTime
  val contactOutcomeCode: String?
  val attendanceData: AttendanceDataDto?
  val notes: String?
  val date: LocalDate
}
