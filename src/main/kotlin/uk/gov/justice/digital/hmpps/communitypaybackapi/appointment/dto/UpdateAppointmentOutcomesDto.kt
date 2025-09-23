package uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

data class UpdateAppointmentOutcomesDto(
  @param:Size(min = 1)
  val ids: List<Long>,
  val outcomeData: UpdateAppointmentOutcomeDto,
) {
  companion object
}

data class UpdateAppointmentOutcomeDto(
  val projectTypeId: Long,
  @param:Schema(example = "09:00", description = "The start local time of the appointment", pattern = "^([0-1][0-9]|2[0-3]):[0-5][0-9]$")
  val startTime: LocalTime,
  @param:Schema(example = "14:00", description = "The end local time of the appointment", pattern = "^([0-1][0-9]|2[0-3]):[0-5][0-9]$")
  val endTime: LocalTime,
  val contactOutcomeId: UUID,
  val supervisorTeamId: Long,
  val supervisorOfficerId: Long,
  val notes: String,
  val attendanceData: UpdateAppointmentAttendanceDataDto?,
  val enforcementData: UpdateAppointmentEnforcementDto?,
) {
  companion object
}

data class UpdateAppointmentAttendanceDataDto(
  val hiVisWarn: Boolean,
  val workedIntensively: Boolean,
  val penaltyMinutes: Long,
  val workQuality: AppointmentWorkQualityDto,
  val behaviour: AppointmentBehaviourDto,
)

data class UpdateAppointmentEnforcementDto(
  val enforcementActionId: UUID,
  val respondBy: LocalDate,
)

enum class AppointmentWorkQualityDto {
  EXCELLENT,
  GOOD,
  NOT_APPLICABLE,
  POOR,
  SATISFACTORY,
  UNSATISFACTORY,
}

enum class AppointmentBehaviourDto {
  EXCELLENT,
  GOOD,
  NOT_APPLICABLE,
  POOR,
  SATISFACTORY,
  UNSATISFACTORY,
}
