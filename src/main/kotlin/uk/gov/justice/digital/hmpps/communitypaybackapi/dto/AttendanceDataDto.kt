package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.HourMinuteDuration

data class AttendanceDataDto(
  val hiVisWorn: Boolean,
  val workedIntensively: Boolean,
  val penaltyTime: HourMinuteDuration? = null,
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
