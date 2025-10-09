package uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity

import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.AppointmentBehaviourDto

enum class Behaviour(val dtoType: AppointmentBehaviourDto) {
  EXCELLENT(AppointmentBehaviourDto.EXCELLENT),
  GOOD(AppointmentBehaviourDto.GOOD),
  NOT_APPLICABLE(AppointmentBehaviourDto.NOT_APPLICABLE),
  POOR(AppointmentBehaviourDto.POOR),
  SATISFACTORY(AppointmentBehaviourDto.SATISFACTORY),
  UNSATISFACTORY(AppointmentBehaviourDto.UNSATISFACTORY),
  ;

  companion object
}
