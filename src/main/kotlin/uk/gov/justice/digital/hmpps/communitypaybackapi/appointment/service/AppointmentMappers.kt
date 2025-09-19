package uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.service

import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.AppointmentBehaviourDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.AppointmentWorkQualityDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.Behaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.WorkQuality

fun WorkQuality.Companion.fromDto(dto: AppointmentWorkQualityDto) = when (dto) {
  AppointmentWorkQualityDto.EXCELLENT -> WorkQuality.EXCELLENT
  AppointmentWorkQualityDto.GOOD -> WorkQuality.GOOD
  AppointmentWorkQualityDto.NOT_APPLICABLE -> WorkQuality.NOT_APPLICABLE
  AppointmentWorkQualityDto.POOR -> WorkQuality.POOR
  AppointmentWorkQualityDto.SATISFACTORY -> WorkQuality.SATISFACTORY
  AppointmentWorkQualityDto.UNSATISFACTORY -> WorkQuality.UNSATISFACTORY
}

fun Behaviour.Companion.fromDto(dto: AppointmentBehaviourDto) = when (dto) {
  AppointmentBehaviourDto.EXCELLENT -> Behaviour.EXCELLENT
  AppointmentBehaviourDto.GOOD -> Behaviour.GOOD
  AppointmentBehaviourDto.NOT_APPLICABLE -> Behaviour.NOT_APPLICABLE
  AppointmentBehaviourDto.POOR -> Behaviour.POOR
  AppointmentBehaviourDto.SATISFACTORY -> Behaviour.SATISFACTORY
  AppointmentBehaviourDto.UNSATISFACTORY -> Behaviour.UNSATISFACTORY
}
