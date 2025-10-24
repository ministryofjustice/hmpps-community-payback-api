package uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.PickUpData
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.PickUpLocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectAppointmentBehaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectAppointmentWorkQuality
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentBehaviourDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentOutcomeDomainEventDetailDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentWorkQualityDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AttendanceDataDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.EnforcementDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.LocationDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.PickUpDataDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.Behaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.WorkQuality
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.OffenderInfoResult

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
  projectName = this.project.name,
  projectCode = this.project.code,
  projectTypeName = this.projectType.name,
  projectTypeCode = this.projectType.code,
  offender = offenderInfoResult.toDto(),
  supervisingTeam = this.team.name,
  supervisingTeamCode = this.team.code,
  providerCode = this.provider.code,
  pickUpData = this.pickUpData?.toDto(),
  date = this.date,
  startTime = this.startTime,
  endTime = this.endTime,
  contactOutcomeId = this.contactOutcomeId,
  attendanceData = AttendanceDataDto(
    hiVisWorn = this.hiVisWorn,
    workedIntensively = this.workedIntensively,
    penaltyTime = this.penaltyTime,
    workQuality = this.workQuality?.toDto(),
    behaviour = this.behaviour?.toDto(),
  ),
  enforcementData = EnforcementDto(
    enforcementActionId = this.enforcementActionId,
    respondBy = this.respondBy,
  ),
  supervisorOfficerCode = this.supervisorOfficerCode,
  notes = this.notes,
)

fun PickUpData.toDto() = PickUpDataDto(
  location = pickUpLocation?.toDto(),
  time = time,
)

fun PickUpLocation.toDto() = LocationDto(
  buildingName = this.buildingName,
  buildingNumber = this.buildingNumber,
  streetName = this.streetName,
  townCity = this.townCity,
  county = this.county,
  postCode = this.postCode,
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
