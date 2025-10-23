package uk.gov.justice.digital.hmpps.communitypaybackapi.entity

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectAppointmentBehaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentBehaviourDto

enum class Behaviour(
  val dtoType: AppointmentBehaviourDto,
  val upstreamType: ProjectAppointmentBehaviour,
) {
  EXCELLENT(AppointmentBehaviourDto.EXCELLENT, ProjectAppointmentBehaviour.EXCELLENT),
  GOOD(AppointmentBehaviourDto.GOOD, ProjectAppointmentBehaviour.GOOD),
  NOT_APPLICABLE(AppointmentBehaviourDto.NOT_APPLICABLE, ProjectAppointmentBehaviour.NOT_APPLICABLE),
  POOR(AppointmentBehaviourDto.POOR, ProjectAppointmentBehaviour.POOR),
  SATISFACTORY(AppointmentBehaviourDto.SATISFACTORY, ProjectAppointmentBehaviour.SATISFACTORY),
  UNSATISFACTORY(AppointmentBehaviourDto.UNSATISFACTORY, ProjectAppointmentBehaviour.UNSATISFACTORY),
  ;

  companion object
}
