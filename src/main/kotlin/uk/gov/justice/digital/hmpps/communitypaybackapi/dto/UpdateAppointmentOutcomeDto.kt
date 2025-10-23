package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalTime
import java.util.UUID

data class UpdateAppointmentOutcomeDto(
  @param:Schema(description = "The version of the appointment retrieved from delius this update is being applied to")
  val deliusVersionToUpdate: UUID,
  @param:Schema(example = "09:00", description = "The start local time of the appointment", pattern = "^([0-1][0-9]|2[0-3]):[0-5][0-9]$")
  val startTime: LocalTime,
  @param:Schema(example = "14:00", description = "The end local time of the appointment", pattern = "^([0-1][0-9]|2[0-3]):[0-5][0-9]$")
  val endTime: LocalTime,
  val contactOutcomeId: UUID,
  val attendanceData: AttendanceDataDto?,
  val enforcementData: EnforcementDto?,
  val supervisorOfficerCode: String,
  val notes: String? = null,
  @param:Schema(description = "If provided, the corresponding form data will be deleted")
  val formKeyToDelete: FormKeyDto?,
  @param:Schema(description = "If the corresponding delius contact should be alerted")
  val alertActive: Boolean,
  @param:Schema(description = "If the corresponding delius contact should be marked as sensitive")
  val sensitive: Boolean,
) {
  companion object
}
