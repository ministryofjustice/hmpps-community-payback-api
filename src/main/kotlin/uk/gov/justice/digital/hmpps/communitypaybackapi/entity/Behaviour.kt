package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentBehaviourDto

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
