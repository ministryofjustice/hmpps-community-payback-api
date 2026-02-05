package uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAppointmentBehaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAppointmentPickUp
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAppointmentSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAppointmentWorkQuality
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCode
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCreateAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDPickUp
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDUpdateAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentBehaviourDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentWorkQualityDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AttendanceDataDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.EnforcementDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.PickUpDataDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.domainevent.AppointmentCreatedDomainEventDetailDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.domainevent.AppointmentDomainEventDetailDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.domainevent.AppointmentUpdatedDomainEventDetailDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.Behaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EnforcementActionEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.WorkQuality

@Service
class AppointmentMappers(
  private val contactOutcomeEntityRepository: ContactOutcomeEntityRepository,
  private val enforcementActionEntityRepository: EnforcementActionEntityRepository,
) {

  fun toDto(
    appointment: NDAppointment,
  ): AppointmentDto {
    val contactOutcomeEntity = appointment.outcome?.code?.let {
      contactOutcomeEntityRepository.findByCode(it) ?: error("Can't find outcome for code $it")
    }

    return AppointmentDto(
      id = appointment.id,
      communityPaybackId = appointment.reference,
      version = appointment.version,
      deliusEventNumber = appointment.event.number,
      projectName = appointment.project.name,
      projectCode = appointment.project.code,
      projectTypeName = appointment.projectType.name,
      projectTypeCode = appointment.projectType.code,
      offender = appointment.case.toDto(),
      supervisingTeam = appointment.team.name,
      supervisingTeamCode = appointment.team.code,
      providerCode = appointment.provider.code,
      pickUpData = appointment.pickUpData?.toDto(),
      date = appointment.date,
      startTime = appointment.startTime,
      endTime = appointment.endTime,
      contactOutcomeCode = contactOutcomeEntity?.code,
      attendanceData = if (contactOutcomeEntity?.attended == true) {
        AttendanceDataDto(
          hiVisWorn = appointment.hiVisWorn!!,
          workedIntensively = appointment.workedIntensively!!,
          penaltyTime = appointment.penaltyHours,
          penaltyMinutes = appointment.penaltyHours?.duration?.toMinutes(),
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

  fun toSummaryDto(
    appointmentSummary: NDAppointmentSummary,
  ) = AppointmentSummaryDto(
    id = appointmentSummary.id,
    contactOutcome = appointmentSummary.outcome?.code?.let {
      contactOutcomeEntityRepository.findByCode(it)?.toDto() ?: error("Can't find outcome for code $it")
    },
    requirementMinutes = appointmentSummary.requirementProgress.requiredMinutes,
    adjustmentMinutes = appointmentSummary.requirementProgress.adjustments,
    completedMinutes = appointmentSummary.requirementProgress.completedMinutes,
    offender = appointmentSummary.case.toDto(),
  )
}

fun AppointmentEventEntity.toAppointmentCreatedDomainEvent() = AppointmentCreatedDomainEventDetailDto(
  appointment = this.toAppointmentDomainEventDetail(),
)

fun AppointmentEventEntity.toAppointmentUpdatedDomainEvent() = AppointmentUpdatedDomainEventDetailDto(
  appointment = this.toAppointmentDomainEventDetail(),
)

private fun AppointmentEventEntity.toAppointmentDomainEventDetail() = AppointmentDomainEventDetailDto(
  id = this.id,
  appointmentDeliusId = this.deliusAppointmentId,
  crn = this.crn,
  deliusEventNumber = this.deliusEventNumber,
  startTime = this.startTime,
  endTime = this.endTime,
  contactOutcomeCode = this.contactOutcome?.code,
  supervisorOfficerCode = this.supervisorOfficerCode,
  notes = this.notes,
  hiVisWorn = this.hiVisWorn,
  workedIntensively = workedIntensively,
  penaltyMinutes = this.penaltyMinutes,
  minutesCredited = this.minutesCredited,
  workQuality = this.workQuality?.dtoType,
  behaviour = this.behaviour?.dtoType,
)

fun AppointmentEventEntity.toNDUpdateAppointment(): NDUpdateAppointment {
  require(this.eventType == AppointmentEventType.UPDATE)
  return NDUpdateAppointment(
    version = this.priorDeliusVersion!!,
    startTime = this.startTime,
    endTime = this.endTime,
    outcome = this.contactOutcome?.let { NDCode(it.code) },
    supervisor = NDCode(this.supervisorOfficerCode!!),
    notes = this.notes,
    hiVisWorn = this.hiVisWorn,
    workedIntensively = workedIntensively,
    penaltyMinutes = this.penaltyMinutes,
    minutesCredited = this.minutesCredited,
    workQuality = this.workQuality?.upstreamType,
    behaviour = this.behaviour?.upstreamType,
    sensitive = this.sensitive,
    alertActive = this.alertActive,
  )
}

fun AppointmentEventEntity.toNDCreateAppointment(): NDCreateAppointment {
  require(this.eventType == AppointmentEventType.CREATE)
  return NDCreateAppointment(
    reference = this.communityPaybackAppointmentId!!,
    crn = this.crn,
    eventNumber = this.deliusEventNumber,
    date = this.date,
    startTime = this.startTime,
    endTime = this.endTime,
    outcome = this.contactOutcome?.let { NDCode(it.code) },
    supervisor = this.supervisorOfficerCode?.let { NDCode(it) },
    notes = this.notes,
    hiVisWorn = this.hiVisWorn,
    workedIntensively = workedIntensively,
    penaltyMinutes = this.penaltyMinutes,
    minutesCredited = this.minutesCredited,
    workQuality = this.workQuality?.upstreamType,
    behaviour = this.behaviour?.upstreamType,
    sensitive = this.sensitive,
    alertActive = this.alertActive,
    pickUp = NDPickUp(
      location = this.pickupLocationCode?.let { NDCode(it) },
      time = this.pickupTime,
    ),
  )
}

fun WorkQuality.Companion.fromDto(dto: AppointmentWorkQualityDto) = WorkQuality.entries.first { it.dtoType == dto }
fun Behaviour.Companion.fromDto(dto: AppointmentBehaviourDto) = Behaviour.entries.first { it.dtoType == dto }

fun NDAppointmentPickUp.toDto() = PickUpDataDto(
  location = location?.toDto(),
  locationCode = locationCode?.code,
  time = time,
)

fun NDAppointmentWorkQuality.toDto() = when (this) {
  NDAppointmentWorkQuality.EXCELLENT -> AppointmentWorkQualityDto.EXCELLENT
  NDAppointmentWorkQuality.GOOD -> AppointmentWorkQualityDto.GOOD
  NDAppointmentWorkQuality.NOT_APPLICABLE -> AppointmentWorkQualityDto.NOT_APPLICABLE
  NDAppointmentWorkQuality.POOR -> AppointmentWorkQualityDto.POOR
  NDAppointmentWorkQuality.SATISFACTORY -> AppointmentWorkQualityDto.SATISFACTORY
  NDAppointmentWorkQuality.UNSATISFACTORY -> AppointmentWorkQualityDto.UNSATISFACTORY
}

fun NDAppointmentBehaviour.toDto() = when (this) {
  NDAppointmentBehaviour.EXCELLENT -> AppointmentBehaviourDto.EXCELLENT
  NDAppointmentBehaviour.GOOD -> AppointmentBehaviourDto.GOOD
  NDAppointmentBehaviour.NOT_APPLICABLE -> AppointmentBehaviourDto.NOT_APPLICABLE
  NDAppointmentBehaviour.POOR -> AppointmentBehaviourDto.POOR
  NDAppointmentBehaviour.SATISFACTORY -> AppointmentBehaviourDto.SATISFACTORY
  NDAppointmentBehaviour.UNSATISFACTORY -> AppointmentBehaviourDto.UNSATISFACTORY
}
