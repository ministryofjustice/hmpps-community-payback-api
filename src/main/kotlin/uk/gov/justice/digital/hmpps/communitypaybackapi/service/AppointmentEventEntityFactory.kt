package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.derivePenaltyMinutesDuration
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.Behaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.WorkQuality
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.fromDto
import java.time.OffsetDateTime
import java.util.UUID

@Service
class AppointmentEventEntityFactory(
  private val providerService: ProviderService,
) {

  fun buildCreatedEvent(
    details: CommunityPaybackSpringEvent.AppointmentCreatedEvent,
  ): AppointmentEventEntity {
    val appointment = details.appointmentEntity
    val createAppointmentDto = details.createDto.dto
    val project = details.createDto.project
    val pickUpLocation = details.createDto.pickUpLocation
    val supervisorCode = createAppointmentDto.supervisorOfficerCode ?: providerService.getTeamUnallocatedSupervisor(project.getTeamId()).code

    return AppointmentEventEntity(
      id = appointment.id,
      appointment = appointment,
      eventType = AppointmentEventType.CREATE,
      priorDeliusVersion = null,
      projectCode = project.projectCode,
      projectName = project.projectName,
      date = createAppointmentDto.date,
      startTime = createAppointmentDto.startTime,
      endTime = createAppointmentDto.endTime,
      pickupLocationCode = pickUpLocation?.deliusCode,
      pickupLocationDescription = pickUpLocation?.description,
      pickupTime = createAppointmentDto.pickUpTime,
      contactOutcome = details.createDto.contactOutcome,
      supervisorOfficerCode = supervisorCode,
      notes = createAppointmentDto.notes,
      hiVisWorn = createAppointmentDto.attendanceData?.hiVisWorn,
      workedIntensively = createAppointmentDto.attendanceData?.workedIntensively,
      penaltyMinutes = createAppointmentDto.attendanceData?.derivePenaltyMinutesDuration()?.toMinutes(),
      minutesCredited = details.createDto.minutesToCredit?.toMinutes(),
      workQuality = createAppointmentDto.attendanceData?.workQuality?.let { WorkQuality.fromDto(it) },
      behaviour = createAppointmentDto.attendanceData?.behaviour?.let { Behaviour.fromDto(it) },
      alertActive = createAppointmentDto.alertActive,
      sensitive = createAppointmentDto.sensitive,
      deliusAllocationId = createAppointmentDto.allocationId,
      triggeredAt = details.trigger.triggeredAt,
      triggerType = details.trigger.triggerType,
      triggeredBy = details.trigger.triggeredBy,
    )
  }

  fun buildUpdatedEvent(
    details: CommunityPaybackSpringEvent.AppointmentUpdatedEvent,
  ): AppointmentEventEntity {
    val existingAppointment = details.existingAppointment
    val outcome = details.updateDto.dto
    val project = details.updateDto.project
    val startTime = outcome.startTime
    val endTime = outcome.endTime
    val penaltyMinutes = outcome.attendanceData?.derivePenaltyMinutesDuration()?.toMinutes()
    val contactOutcome = details.updateDto.contactOutcome

    return AppointmentEventEntity(
      id = UUID.randomUUID(),
      appointment = details.appointmentEntity,
      eventType = AppointmentEventType.UPDATE,
      priorDeliusVersion = outcome.deliusVersionToUpdate,
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
      minutesCredited = details.updateDto.minutesToCredit?.toMinutes(),
      workQuality = outcome.attendanceData?.workQuality?.let { WorkQuality.fromDto(it) },
      behaviour = outcome.attendanceData?.behaviour?.let { Behaviour.fromDto(it) },
      alertActive = outcome.alertActive,
      sensitive = outcome.sensitive,
      deliusAllocationId = null,
      triggeredAt = details.trigger.triggeredAt,
      triggerType = details.trigger.triggerType,
      triggeredBy = details.trigger.triggeredBy,
    )
  }
}

data class AppointmentEventTrigger(
  val triggeredAt: OffsetDateTime = OffsetDateTime.now(),
  val triggerType: AppointmentEventTriggerType,
  val triggeredBy: String,
) {
  companion object
}
