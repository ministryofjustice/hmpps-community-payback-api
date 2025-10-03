package uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.service

import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.AppointmentBehaviourDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.AppointmentOutcomeDomainEventDetailDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.AppointmentWorkQualityDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.AttendanceDataDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.EnforcementDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.AppointmentOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.Behaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.WorkQuality
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAppointmentBehaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAppointmentWorkQuality
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.OffenderInfoResult
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.toDto

fun AppointmentOutcomeEntity.toDomainEventDetail() = AppointmentOutcomeDomainEventDetailDto(
  id = this.id,
  appointmentDeliusId = this.appointmentDeliusId,
  startTime = this.startTime,
  endTime = this.endTime,
  contactOutcomeCode = this.contactOutcomeEntity!!.code,
  supervisorOfficerCode = this.supervisorOfficerCode,
  notes = this.notes,
  hiVisWorn = this.hiVisWorn,
  workedIntensively = workedIntensively,
  penaltyMinutes = this.penaltyMinutes,
  workQuality = this.workQuality?.dtoType,
  behaviour = this.behaviour?.dtoType,
  enforcementActionCode = this.enforcementActionEntity!!.code,
  respondBy = this.respondBy,
)

fun WorkQuality.Companion.fromDto(dto: AppointmentWorkQualityDto) = WorkQuality.entries.first { it.dtoType == dto }
fun Behaviour.Companion.fromDto(dto: AppointmentBehaviourDto) = Behaviour.entries.first { it.dtoType == dto }

fun ProjectAppointment.toDto(offenderInfoResult: OffenderInfoResult) = AppointmentDto(
  id = this.id,
  projectName = this.projectName,
  projectCode = this.projectCode,
  offender = offenderInfoResult.toDto(),
  supervisingTeam = this.supervisingTeam,
  date = this.date,
  startTime = this.startTime,
  endTime = this.endTime,
  attendanceData = AttendanceDataDto(
    hiVisWorn = this.hiVisWorn,
    workedIntensively = this.workedIntensively,
    penaltyTime = this.penaltyTime,
    workQuality = this.workQuality?.toDto(),
    behaviour = this.behaviour?.toDto(),
    supervisorOfficerCode = this.supervisorCode,
    contactOutcomeId = this.contactOutcomeId,
  ),
  enforcementData = EnforcementDto(
    enforcementActionId = this.enforcementActionId,
    respondBy = this.respondBy,
  ),
  notes = this.notes,
)

fun ProjectAppointmentWorkQuality.toDto() = when (this) {
  ProjectAppointmentWorkQuality.EXCELLENT -> AppointmentWorkQualityDto.EXCELLENT
  ProjectAppointmentWorkQuality.GOOD -> AppointmentWorkQualityDto.GOOD
  ProjectAppointmentWorkQuality.NOT_APPLICABLE -> AppointmentWorkQualityDto.NOT_APPLICABLE
  ProjectAppointmentWorkQuality.POOR -> AppointmentWorkQualityDto.POOR
  ProjectAppointmentWorkQuality.SATISFACTORY -> AppointmentWorkQualityDto.SATISFACTORY
  ProjectAppointmentWorkQuality.UNSATISFACTORY -> AppointmentWorkQualityDto.UNSATISFACTORY
}

fun ProjectAppointmentBehaviour.toDto() = when (this) {
  ProjectAppointmentBehaviour.EXCELLENT -> AppointmentBehaviourDto.EXCELLENT
  ProjectAppointmentBehaviour.GOOD -> AppointmentBehaviourDto.GOOD
  ProjectAppointmentBehaviour.NOT_APPLICABLE -> AppointmentBehaviourDto.NOT_APPLICABLE
  ProjectAppointmentBehaviour.POOR -> AppointmentBehaviourDto.POOR
  ProjectAppointmentBehaviour.SATISFACTORY -> AppointmentBehaviourDto.SATISFACTORY
  ProjectAppointmentBehaviour.UNSATISFACTORY -> AppointmentBehaviourDto.UNSATISFACTORY
}
