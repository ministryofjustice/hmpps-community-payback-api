package uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.service

import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.AppointmentBehaviourDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.AppointmentOutcomeDomainEventDetailDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.AppointmentWorkQualityDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.AppointmentOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.Behaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.WorkQuality

fun AppointmentOutcomeEntity.toDomainEventDetail() = AppointmentOutcomeDomainEventDetailDto(
  id = this.id,
  appointmentDeliusId = this.appointmentDeliusId,
  projectTypeDeliusId = this.projectTypeDeliusId,
  startTime = this.startTime,
  endTime = this.endTime,
  contactOutcomeDeliusCode = this.contactOutcomeEntity!!.code,
  supervisorTeamDeliusId = this.supervisorTeamDeliusId,
  supervisorOfficerDeliusId = this.supervisorOfficerDeliusId,
  notes = this.notes,
  hiVisWorn = this.hiVisWorn,
  workedIntensively = workedIntensively,
  penaltyMinutes = this.penaltyMinutes,
  workQuality = this.workQuality?.dtoType,
  behaviour = this.behaviour?.dtoType,
  enforcementActionDeliusCode = this.enforcementActionEntity!!.code,
  respondBy = this.respondBy,
)

fun WorkQuality.Companion.fromDto(dto: AppointmentWorkQualityDto) = WorkQuality.entries.first { it.dtoType == dto }
fun Behaviour.Companion.fromDto(dto: AppointmentBehaviourDto) = Behaviour.entries.first { it.dtoType == dto }
