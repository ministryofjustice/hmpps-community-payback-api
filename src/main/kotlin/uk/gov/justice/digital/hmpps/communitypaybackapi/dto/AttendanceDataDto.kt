package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.HourMinuteDuration

data class AttendanceDataDto(
  val hiVisWorn: Boolean,
  val workedIntensively: Boolean,
  @Deprecated("use penaltyMinutes")
  @param:Schema(description = "Deprecated, use penaltyMinutes instead", deprecated = true)
  val penaltyTime: HourMinuteDuration? = null,
  val penaltyMinutes: Long? = null,
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
