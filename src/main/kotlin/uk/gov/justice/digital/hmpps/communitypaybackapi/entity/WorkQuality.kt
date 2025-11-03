package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.AppointmentWorkQuality
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentWorkQualityDto

enum class WorkQuality(
  val dtoType: AppointmentWorkQualityDto,
  val upstreamType: AppointmentWorkQuality,
) {
  EXCELLENT(AppointmentWorkQualityDto.EXCELLENT, AppointmentWorkQuality.EXCELLENT),
  GOOD(AppointmentWorkQualityDto.GOOD, AppointmentWorkQuality.GOOD),
  NOT_APPLICABLE(AppointmentWorkQualityDto.NOT_APPLICABLE, AppointmentWorkQuality.NOT_APPLICABLE),
  POOR(AppointmentWorkQualityDto.POOR, AppointmentWorkQuality.POOR),
  SATISFACTORY(AppointmentWorkQualityDto.SATISFACTORY, AppointmentWorkQuality.SATISFACTORY),
  UNSATISFACTORY(AppointmentWorkQualityDto.UNSATISFACTORY, AppointmentWorkQuality.UNSATISFACTORY),
  ;

  companion object
}
