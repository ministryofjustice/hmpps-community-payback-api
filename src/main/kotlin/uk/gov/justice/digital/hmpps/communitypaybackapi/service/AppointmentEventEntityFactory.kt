package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.derivePenaltyMinutesDuration
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.Behaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.WorkQuality
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.fromDto
import java.time.OffsetDateTime
import java.util.UUID

@Service
class AppointmentEventEntityFactory(
  private val contactOutcomeEntityRepository: ContactOutcomeEntityRepository,
  private val projectService: ProjectService,
  private val providerService: ProviderService,
) {

  fun buildCreatedEvent(
    deliusId: Long,
    trigger: AppointmentEventTrigger,
    validatedCreateAppointmentDto: Validated<CreateAppointmentDto>,
  ): AppointmentEventEntity {
    val createAppointmentDto = validatedCreateAppointmentDto.value
    val project = validatedCreateAppointmentDto.project
    val supervisorCode = createAppointmentDto.supervisorOfficerCode ?: providerService.getTeamUnallocatedSupervisor(project.getTeamId()).code

    return AppointmentEventEntity(
      id = createAppointmentDto.id,
      communityPaybackAppointmentId = createAppointmentDto.id,
      eventType = AppointmentEventType.CREATE,
      crn = createAppointmentDto.crn,
      deliusAppointmentId = deliusId,
      priorDeliusVersion = null,
      deliusEventNumber = createAppointmentDto.deliusEventNumber.toInt(),
      projectCode = project.projectCode,
      projectName = project.projectName,
      date = createAppointmentDto.date,
      startTime = createAppointmentDto.startTime,
      endTime = createAppointmentDto.endTime,
      pickupLocationCode = createAppointmentDto.pickUpLocationCode,
      pickupLocationDescription = createAppointmentDto.pickUpLocationDescription,
      pickupTime = createAppointmentDto.pickUpTime,
      contactOutcome = validatedCreateAppointmentDto.contactOutcome,
      supervisorOfficerCode = supervisorCode,
      notes = createAppointmentDto.notes,
      hiVisWorn = createAppointmentDto.attendanceData?.hiVisWorn,
      workedIntensively = createAppointmentDto.attendanceData?.workedIntensively,
      penaltyMinutes = createAppointmentDto.attendanceData?.derivePenaltyMinutesDuration()?.toMinutes(),
      minutesCredited = validatedCreateAppointmentDto.minutesToCredit?.toMinutes(),
      workQuality = createAppointmentDto.attendanceData?.workQuality?.let { WorkQuality.fromDto(it) },
      behaviour = createAppointmentDto.attendanceData?.behaviour?.let { Behaviour.fromDto(it) },
      alertActive = createAppointmentDto.alertActive,
      sensitive = createAppointmentDto.sensitive,
      deliusAllocationId = createAppointmentDto.allocationId,
      triggeredAt = trigger.triggeredAt,
      triggerType = trigger.triggerType,
      triggeredBy = trigger.triggeredBy,
    )
  }

  fun buildUpdatedEvent(
    validatedUpdate: Validated<UpdateAppointmentOutcomeDto>,
    existingAppointment: AppointmentDto,
    trigger: AppointmentEventTrigger,
    projectCode: String,
  ): AppointmentEventEntity {
    val outcome = validatedUpdate.value
    val project = projectService.getProject(projectCode)
    val startTime = outcome.startTime
    val endTime = outcome.endTime
    val penaltyMinutes = outcome.attendanceData?.derivePenaltyMinutesDuration()?.toMinutes()
    val contactOutcome = loadOutcome(outcome.contactOutcomeCode)

    return AppointmentEventEntity(
      id = UUID.randomUUID(),
      communityPaybackAppointmentId = existingAppointment.communityPaybackId,
      eventType = AppointmentEventType.UPDATE,
      crn = existingAppointment.offender.crn,
      deliusAppointmentId = outcome.deliusId,
      priorDeliusVersion = outcome.deliusVersionToUpdate,
      deliusEventNumber = existingAppointment.deliusEventNumber,
      projectCode = project.projectCode,
      projectName = project.projectName,
      date = existingAppointment.date,
      startTime = startTime,
      endTime = endTime,
      pickupLocationCode = existingAppointment.pickUpData?.locationCode,
      pickupLocationDescription = existingAppointment.pickUpData?.locationDescription,
      pickupTime = existingAppointment.pickUpData?.time,
      contactOutcome = contactOutcome,
      supervisorOfficerCode = outcome.supervisorOfficerCode,
      notes = outcome.notes,
      hiVisWorn = outcome.attendanceData?.hiVisWorn,
      workedIntensively = outcome.attendanceData?.workedIntensively,
      penaltyMinutes = penaltyMinutes,
      minutesCredited = validatedUpdate.minutesToCredit?.toMinutes(),
      workQuality = outcome.attendanceData?.workQuality?.let { WorkQuality.fromDto(it) },
      behaviour = outcome.attendanceData?.behaviour?.let { Behaviour.fromDto(it) },
      alertActive = outcome.alertActive,
      sensitive = outcome.sensitive,
      deliusAllocationId = null,
      triggeredAt = trigger.triggeredAt,
      triggerType = trigger.triggerType,
      triggeredBy = trigger.triggeredBy,
    )
  }

  private fun loadOutcome(code: String?) = code?.let {
    contactOutcomeEntityRepository.findByCode(it) ?: error("ContactOutcome not found for code: $it")
  }
}

data class AppointmentEventTrigger(
  val triggeredAt: OffsetDateTime = OffsetDateTime.now(),
  val triggerType: AppointmentEventTriggerType,
  val triggeredBy: String,
) {
  companion object
}
