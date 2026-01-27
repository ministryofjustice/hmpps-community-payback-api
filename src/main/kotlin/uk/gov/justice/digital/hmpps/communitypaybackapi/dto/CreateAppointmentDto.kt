package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

data class CreateAppointmentDto(
  val id: UUID,
  val crn: String,
  val deliusEventNumber: Int,
  val allocationId: Long?,
  val date: LocalDate,
  @param:Schema(example = "09:00", description = "The start local time of the appointment", pattern = "^([0-1][0-9]|2[0-3]):[0-5][0-9]$")
  val startTime: LocalTime,
  @param:Schema(example = "14:00", description = "The end local time of the appointment", pattern = "^([0-1][0-9]|2[0-3]):[0-5][0-9]$")
  val endTime: LocalTime,
  val contactOutcomeCode: String? = null,
  val attendanceData: AttendanceDataDto? = null,
  @param:Schema(description = "Will default to the unallocated supervisor for the project's team")
  val supervisorOfficerCode: String? = null,
  val notes: String? = null,
  @param:Schema(description = "If the corresponding delius contact should be alerted")
  val alertActive: Boolean? = null,
  @param:Schema(description = "If the corresponding delius contact should be marked as sensitive")
  val sensitive: Boolean? = null,
) {
  companion object
}
