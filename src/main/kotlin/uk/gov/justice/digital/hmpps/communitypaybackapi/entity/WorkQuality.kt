package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAppointmentWorkQuality
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentWorkQualityDto

enum class WorkQuality(
  val dtoType: AppointmentWorkQualityDto,
  val upstreamType: NDAppointmentWorkQuality,
) {
  EXCELLENT(AppointmentWorkQualityDto.EXCELLENT, NDAppointmentWorkQuality.EXCELLENT),
  GOOD(AppointmentWorkQualityDto.GOOD, NDAppointmentWorkQuality.GOOD),
  NOT_APPLICABLE(AppointmentWorkQualityDto.NOT_APPLICABLE, NDAppointmentWorkQuality.NOT_APPLICABLE),
  POOR(AppointmentWorkQualityDto.POOR, NDAppointmentWorkQuality.POOR),
  SATISFACTORY(AppointmentWorkQualityDto.SATISFACTORY, NDAppointmentWorkQuality.SATISFACTORY),
  UNSATISFACTORY(AppointmentWorkQualityDto.UNSATISFACTORY, NDAppointmentWorkQuality.UNSATISFACTORY),
  ;

  companion object
}
