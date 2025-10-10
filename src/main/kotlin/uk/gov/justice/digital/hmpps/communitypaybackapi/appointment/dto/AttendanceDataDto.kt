package uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto

import java.util.UUID

data class AttendanceDataDto(
  val hiVisWorn: Boolean? = null,
  val workedIntensively: Boolean? = null,
  val penaltyMinutes: Long? = null,
  val workQuality: AppointmentWorkQualityDto? = null,
  val behaviour: AppointmentBehaviourDto? = null,
  val supervisorOfficerCode: String? = null,
  val contactOutcomeId: UUID? = null,
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
