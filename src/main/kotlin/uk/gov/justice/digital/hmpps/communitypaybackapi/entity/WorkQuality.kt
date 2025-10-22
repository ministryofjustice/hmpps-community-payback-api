package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentWorkQualityDto

enum class WorkQuality(val dtoType: AppointmentWorkQualityDto) {
  EXCELLENT(AppointmentWorkQualityDto.EXCELLENT),
  GOOD(AppointmentWorkQualityDto.GOOD),
  NOT_APPLICABLE(AppointmentWorkQualityDto.NOT_APPLICABLE),
  POOR(AppointmentWorkQualityDto.POOR),
  SATISFACTORY(AppointmentWorkQualityDto.SATISFACTORY),
  UNSATISFACTORY(AppointmentWorkQualityDto.UNSATISFACTORY),
  ;

  companion object
}
