package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectAppointmentWorkQuality
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentWorkQualityDto

enum class WorkQuality(
  val dtoType: AppointmentWorkQualityDto,
  val upstreamType: ProjectAppointmentWorkQuality,
) {
  EXCELLENT(AppointmentWorkQualityDto.EXCELLENT, ProjectAppointmentWorkQuality.EXCELLENT),
  GOOD(AppointmentWorkQualityDto.GOOD, ProjectAppointmentWorkQuality.GOOD),
  NOT_APPLICABLE(AppointmentWorkQualityDto.NOT_APPLICABLE, ProjectAppointmentWorkQuality.NOT_APPLICABLE),
  POOR(AppointmentWorkQualityDto.POOR, ProjectAppointmentWorkQuality.POOR),
  SATISFACTORY(AppointmentWorkQualityDto.SATISFACTORY, ProjectAppointmentWorkQuality.SATISFACTORY),
  UNSATISFACTORY(AppointmentWorkQualityDto.UNSATISFACTORY, ProjectAppointmentWorkQuality.UNSATISFACTORY),
  ;

  companion object
}
