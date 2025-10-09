package uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity

import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.AppointmentWorkQualityDto

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
