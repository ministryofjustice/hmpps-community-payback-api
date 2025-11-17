package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

data class AppointmentDto(
  val id: Long,
  val version: UUID,
  val projectName: String,
  val projectCode: String,
  val projectTypeName: String,
  val projectTypeCode: String,
  val offender: OffenderDto,
  val supervisingTeam: String,
  val supervisingTeamCode: String,
  val providerCode: String,
  val pickUpData: PickUpDataDto?,
  val date: LocalDate,
  val startTime: LocalTime,
  val endTime: LocalTime,
  val contactOutcomeId: UUID? = null,
  val attendanceData: AttendanceDataDto?,
  val enforcementData: EnforcementDto?,
  val supervisorOfficerCode: String,
  val notes: String?,
  val sensitive: Boolean?,
  val alertActive: Boolean?,
) {
  companion object
}
