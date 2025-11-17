package uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Appointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.AppointmentBehaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.AppointmentSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.AppointmentWorkQuality
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.PickUpData
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.UpdateAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentBehaviourDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentOutcomeDomainEventDetailDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentWorkQualityDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AttendanceDataDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.EnforcementDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.PickUpDataDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.Behaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EnforcementActionEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.WorkQuality
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.OffenderInfoResult
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.OffenderService

@Service
class AppointmentMappers(
  private val offenderService: OffenderService,
  private val contactOutcomeEntityRepository: ContactOutcomeEntityRepository,
  private val enforcementActionEntityRepository: EnforcementActionEntityRepository,
) {

  fun toDto(
    appointment: Appointment,
  ): AppointmentDto {
    val contactOutcomeEntity = appointment.outcome?.code?.let {
      contactOutcomeEntityRepository.findByCode(it) ?: error("Can't find outcome for code $it")
    }

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
      contactOutcomeId = contactOutcomeEntity?.id,
      attendanceData = if (contactOutcomeEntity?.attended == true) {
        AttendanceDataDto(
          hiVisWorn = appointment.hiVisWorn!!,
          workedIntensively = appointment.workedIntensively!!,
          penaltyTime = appointment.penaltyHours,
          workQuality = appointment.workQuality!!.toDto(),
          behaviour = appointment.behaviour!!.toDto(),
        )
      } else {
        null
      },
      enforcementData = appointment.enforcementAction?.let {
        EnforcementDto(
          enforcementActionId = enforcementActionEntityRepository.findByCode(it.code)?.id ?: error("Can't find enforcement action for code: ${it.code}"),
          respondBy = it.respondBy,
        )
      },
      supervisorOfficerCode = appointment.supervisor.code,
      notes = appointment.notes,
      sensitive = appointment.sensitive,
      alertActive = appointment.alertActive,
    )
  }

  fun toDto(
    appointmentSummary: AppointmentSummary,
    offenderInfoResult: OffenderInfoResult,
  ) = AppointmentSummaryDto(
    id = appointmentSummary.id,
    startTime = appointmentSummary.startTime,
    endTime = appointmentSummary.endTime,
    contactOutcome = appointmentSummary.outcome?.code?.let {
      contactOutcomeEntityRepository.findByCode(it)?.toDto() ?: error("Can't find outcome for code $it")
    },
    requirementMinutes = appointmentSummary.requirementProgress.requirementMinutes,
    completedMinutes = appointmentSummary.requirementProgress.completedMinutes,
    offender = offenderInfoResult.toDto(),
  )
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
  enforcementActionCode = this.enforcementAction?.code,
  respondBy = this.respondBy,
)

fun WorkQuality.Companion.fromDto(dto: AppointmentWorkQualityDto) = WorkQuality.entries.first { it.dtoType == dto }
fun Behaviour.Companion.fromDto(dto: AppointmentBehaviourDto) = Behaviour.entries.first { it.dtoType == dto }

fun PickUpData.toDto() = PickUpDataDto(
  location = pickUpLocation?.toDto(),
  time = time,
)

fun AppointmentWorkQuality.toDto() = when (this) {
  AppointmentWorkQuality.EXCELLENT -> AppointmentWorkQualityDto.EXCELLENT
  AppointmentWorkQuality.GOOD -> AppointmentWorkQualityDto.GOOD
  AppointmentWorkQuality.NOT_APPLICABLE -> AppointmentWorkQualityDto.NOT_APPLICABLE
  AppointmentWorkQuality.POOR -> AppointmentWorkQualityDto.POOR
  AppointmentWorkQuality.SATISFACTORY -> AppointmentWorkQualityDto.SATISFACTORY
  AppointmentWorkQuality.UNSATISFACTORY -> AppointmentWorkQualityDto.UNSATISFACTORY
}

fun AppointmentBehaviour.toDto() = when (this) {
  AppointmentBehaviour.EXCELLENT -> AppointmentBehaviourDto.EXCELLENT
  AppointmentBehaviour.GOOD -> AppointmentBehaviourDto.GOOD
  AppointmentBehaviour.NOT_APPLICABLE -> AppointmentBehaviourDto.NOT_APPLICABLE
  AppointmentBehaviour.POOR -> AppointmentBehaviourDto.POOR
  AppointmentBehaviour.SATISFACTORY -> AppointmentBehaviourDto.SATISFACTORY
  AppointmentBehaviour.UNSATISFACTORY -> AppointmentBehaviourDto.UNSATISFACTORY
}
