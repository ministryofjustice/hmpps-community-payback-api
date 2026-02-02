package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAppointmentBehaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentBehaviourDto

enum class Behaviour(
  val dtoType: AppointmentBehaviourDto,
  val upstreamType: NDAppointmentBehaviour,
) {
  EXCELLENT(AppointmentBehaviourDto.EXCELLENT, NDAppointmentBehaviour.EXCELLENT),
  GOOD(AppointmentBehaviourDto.GOOD, NDAppointmentBehaviour.GOOD),
  NOT_APPLICABLE(AppointmentBehaviourDto.NOT_APPLICABLE, NDAppointmentBehaviour.NOT_APPLICABLE),
  POOR(AppointmentBehaviourDto.POOR, NDAppointmentBehaviour.POOR),
  SATISFACTORY(AppointmentBehaviourDto.SATISFACTORY, NDAppointmentBehaviour.SATISFACTORY),
  UNSATISFACTORY(AppointmentBehaviourDto.UNSATISFACTORY, NDAppointmentBehaviour.UNSATISFACTORY),
  ;

  companion object
}
