package uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.PickUpData
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.PickUpLocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectAppointmentBehaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectAppointmentWorkQuality
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.UpdateAppointment
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
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EnforcementActionEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.WorkQuality
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.OffenderService

@Service
class AppointmentMappers(
  private val offenderService: OffenderService,
  private val contactOutcomeEntityRepository: ContactOutcomeEntityRepository,
  private val enforcementActionEntityRepository: EnforcementActionEntityRepository,
) {

  fun toDto(
    appointment: ProjectAppointment,
  ): AppointmentDto {
    val offenderInfoResult = offenderService.toOffenderInfo(appointment.case)

    return AppointmentDto(
      id = appointment.id,
      version = appointment.version,
      projectName = appointment.project.name,
      projectCode = appointment.project.code,
      projectTypeName = appointment.projectType.name,
      projectTypeCode = appointment.projectType.code,
      offender = offenderInfoResult.toDto(),
      supervisingTeam = appointment.team.name,
      supervisingTeamCode = appointment.team.code,
      providerCode = appointment.provider.code,
      pickUpData = appointment.pickUpData?.toDto(),
      date = appointment.date,
      startTime = appointment.startTime,
      endTime = appointment.endTime,
      contactOutcomeId = appointment.outcome?.code?.let {
        contactOutcomeEntityRepository.findByCode(it)?.id ?: error("Can't find outcome for code $it")
      },
      attendanceData = AttendanceDataDto(
        hiVisWorn = appointment.hiVisWorn,
        workedIntensively = appointment.workedIntensively,
        penaltyTime = appointment.penaltyTime,
        workQuality = appointment.workQuality?.toDto(),
        behaviour = appointment.behaviour?.toDto(),
      ),
      enforcementData = appointment.enforcementAction?.let {
        EnforcementDto(
          enforcementActionId = enforcementActionEntityRepository.findByCode(it.code)?.id ?: error("Can't find enforcement action for code: ${it.code}"),
          respondBy = it.respondBy,
        )
      },
      supervisorOfficerCode = appointment.supervisor?.code,
      notes = appointment.notes,
      sensitive = appointment.sensitive,
      alertActive = appointment.alertActive,
    )
  }
}

fun AppointmentOutcomeEntity.toDomainEventDetail() = AppointmentOutcomeDomainEventDetailDto(
  id = this.id,
  appointmentDeliusId = this.appointmentDeliusId,
  startTime = this.startTime,
  endTime = this.endTime,
  contactOutcomeCode = this.contactOutcome.code,
  supervisorOfficerCode = this.supervisorOfficerCode,
  notes = this.notes,
  hiVisWorn = this.hiVisWorn,
  workedIntensively = workedIntensively,
  penaltyMinutes = this.penaltyMinutes,
  workQuality = this.workQuality?.dtoType,
  behaviour = this.behaviour?.dtoType,
  enforcementActionCode = this.enforcementAction!!.code,
  respondBy = this.respondBy,
)

fun AppointmentOutcomeEntity.toUpdateAppointment() = UpdateAppointment(
  version = this.deliusVersionToUpdate,
  startTime = this.startTime,
  endTime = this.endTime,
  contactOutcomeCode = this.contactOutcome.code,
  supervisorOfficerCode = this.supervisorOfficerCode,
  notes = this.notes,
  hiVisWorn = this.hiVisWorn,
  workedIntensively = workedIntensively,
  penaltyMinutes = this.penaltyMinutes,
  workQuality = this.workQuality?.upstreamType,
  behaviour = this.behaviour?.upstreamType,
  sensitive = this.sensitive,
  alertActive = this.alertActive,
  enforcementActionCode = this.enforcementAction!!.code,
  respondBy = this.respondBy,
)

fun WorkQuality.Companion.fromDto(dto: AppointmentWorkQualityDto) = WorkQuality.entries.first { it.dtoType == dto }
fun Behaviour.Companion.fromDto(dto: AppointmentBehaviourDto) = Behaviour.entries.first { it.dtoType == dto }

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
