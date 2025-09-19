package uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto

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

// DA: I think alot of these IDs should maybe be codes?
data class UpdateAppointmentOutcomeDto(
  val projectTypeId: Long,
  val startTime: LocalTime,
  val endTime: LocalTime,
  val contactOutcomeId: UUID,
  // DA: is this redundant?
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
  val enforcementActionId: Long,
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
