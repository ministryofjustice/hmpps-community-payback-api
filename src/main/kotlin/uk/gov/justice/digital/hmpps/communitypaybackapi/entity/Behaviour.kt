package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.AppointmentBehaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentBehaviourDto

enum class Behaviour(
  val dtoType: AppointmentBehaviourDto,
  val upstreamType: AppointmentBehaviour,
) {
  EXCELLENT(AppointmentBehaviourDto.EXCELLENT, AppointmentBehaviour.EXCELLENT),
  GOOD(AppointmentBehaviourDto.GOOD, AppointmentBehaviour.GOOD),
  NOT_APPLICABLE(AppointmentBehaviourDto.NOT_APPLICABLE, AppointmentBehaviour.NOT_APPLICABLE),
  POOR(AppointmentBehaviourDto.POOR, AppointmentBehaviour.POOR),
  SATISFACTORY(AppointmentBehaviourDto.SATISFACTORY, AppointmentBehaviour.SATISFACTORY),
  UNSATISFACTORY(AppointmentBehaviourDto.UNSATISFACTORY, AppointmentBehaviour.UNSATISFACTORY),
  ;

  companion object
}
