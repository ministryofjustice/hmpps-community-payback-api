package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import java.time.LocalTime

data class AttendanceDataDto(
  val hiVisWorn: Boolean,
  val workedIntensively: Boolean,
  val penaltyTime: LocalTime? = null,
  val workQuality: AppointmentWorkQualityDto,
  val behaviour: AppointmentBehaviourDto,
) {
  companion object
}

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
