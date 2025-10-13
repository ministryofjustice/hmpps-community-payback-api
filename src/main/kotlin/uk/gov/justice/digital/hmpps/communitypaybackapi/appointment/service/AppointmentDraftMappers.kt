package uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.service

import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.AppointmentDraftDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.AttendanceDataDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.EnforcementDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.AppointmentDraftEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.dto.ContactOutcomeDto

fun AppointmentDraftEntity.toDto() = AppointmentDraftDto(
  id = this.id,
  appointmentDeliusId = this.appointmentDeliusId,
  crn = this.crn,
  projectName = this.projectName,
  projectCode = this.projectCode,
  projectTypeId = this.projectTypeId,
  projectTypeName = this.projectTypeEntity?.name,
  projectTypeCode = this.projectTypeEntity?.code,
  supervisingTeamCode = this.supervisingTeamCode,
  appointmentDate = this.appointmentDate,
  startTime = this.startTime,
  endTime = this.endTime,
  attendanceData = this.toAttendanceDataDto(),
  contactOutcome = this.contactOutcomeEntity?.let { ContactOutcomeDto(it.id, it.name, it.code) },
  enforcementData = this.toEnforcementDto(),
  notes = this.notes,
  deliusLastUpdatedAt = this.deliusLastUpdatedAt,
  createdAt = this.createdAt,
  updatedAt = this.updatedAt,
)

private fun AppointmentDraftEntity.toAttendanceDataDto(): AttendanceDataDto? = takeIf {
  listOf(
    hiVisWorn,
    workedIntensively,
    penaltyTimeMinutes,
    workQuality,
    behaviour,
    supervisorOfficerCode,
    contactOutcomeId,
  ).any { it != null }
}?.let {
  AttendanceDataDto(
    hiVisWorn = hiVisWorn,
    workedIntensively = workedIntensively,
    penaltyMinutes = penaltyTimeMinutes,
    workQuality = workQuality?.dtoType,
    behaviour = behaviour?.dtoType,
    supervisorOfficerCode = supervisorOfficerCode,
    contactOutcomeId = contactOutcomeId,
  )
}

private fun AppointmentDraftEntity.toEnforcementDto(): EnforcementDto? = if (this.enforcementActionId != null || this.respondBy != null) {
  EnforcementDto(
    enforcementActionId = this.enforcementActionId,
    respondBy = this.respondBy,
  )
} else {
  null
}
