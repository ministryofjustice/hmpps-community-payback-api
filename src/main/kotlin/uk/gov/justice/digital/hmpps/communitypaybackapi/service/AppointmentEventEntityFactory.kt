package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AttendanceDataDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.Behaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.WorkQuality
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.fromDto
import java.time.Duration
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.UUID

@Service
class AppointmentEventEntityFactory(
  private val contactOutcomeEntityRepository: ContactOutcomeEntityRepository,
) {

  fun buildCreatedEvent(
    deliusId: Long,
    trigger: AppointmentEventTrigger,
    createAppointmentDto: CreateAppointmentDto,
    project: ProjectDto,
  ): AppointmentEventEntity {
    val startTime = createAppointmentDto.startTime
    val endTime = createAppointmentDto.endTime
    val penaltyMinutes = createAppointmentDto.attendanceData?.derivePenaltyMinutesDuration()?.toMinutes()
    val contactOutcome = loadOutcome(createAppointmentDto.contactOutcomeCode)

    return AppointmentEventEntity(
      id = UUID.randomUUID(),
      communityPaybackAppointmentId = createAppointmentDto.id,
      eventType = AppointmentEventType.CREATE,
      crn = createAppointmentDto.crn,
      deliusAppointmentId = deliusId,
      priorDeliusVersion = null,
      deliusEventNumber = createAppointmentDto.deliusEventNumber,
      projectCode = project.projectCode,
      projectName = project.projectName,
      date = createAppointmentDto.date,
      startTime = startTime,
      endTime = endTime,
      pickupLocationCode = createAppointmentDto.pickUpLocationCode,
      pickupTime = createAppointmentDto.pickUpTime,
      contactOutcome = contactOutcome,
      supervisorOfficerCode = createAppointmentDto.supervisorOfficerCode,
      notes = createAppointmentDto.notes,
      hiVisWorn = createAppointmentDto.attendanceData?.hiVisWorn,
      workedIntensively = createAppointmentDto.attendanceData?.workedIntensively,
      penaltyMinutes = penaltyMinutes,
      minutesCredited = calculateMinutesCredited(
        startTime = startTime,
        endTime = endTime,
        penaltyMinutes = penaltyMinutes,
        contactOutcome = contactOutcome,
      ),
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
    outcome: UpdateAppointmentOutcomeDto,
    existingAppointment: AppointmentDto,
    trigger: AppointmentEventTrigger,
  ): AppointmentEventEntity {
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
      projectCode = existingAppointment.projectCode,
      projectName = existingAppointment.projectName!!,
      date = existingAppointment.date,
      startTime = startTime,
      endTime = endTime,
      pickupLocationCode = existingAppointment.pickUpData?.locationCode,
      pickupTime = existingAppointment.pickUpData?.time,
      contactOutcome = contactOutcome,
      supervisorOfficerCode = outcome.supervisorOfficerCode,
      notes = outcome.notes,
      hiVisWorn = outcome.attendanceData?.hiVisWorn,
      workedIntensively = outcome.attendanceData?.workedIntensively,
      penaltyMinutes = penaltyMinutes,
      minutesCredited = calculateMinutesCredited(
        startTime = startTime,
        endTime = endTime,
        penaltyMinutes = penaltyMinutes,
        contactOutcome = contactOutcome,
      ),
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

  private fun calculateMinutesCredited(
    startTime: LocalTime,
    endTime: LocalTime,
    penaltyMinutes: Long?,
    contactOutcome: ContactOutcomeEntity?,
  ): Long? {
    if (contactOutcome?.attended != true) return null

    val minutesCredited = Duration.between(startTime, endTime).toMinutes() - (penaltyMinutes ?: 0L)
    return minutesCredited.takeIf { it != 0L }
  }
}

fun AttendanceDataDto.derivePenaltyMinutesDuration() = penaltyMinutes?.let { Duration.ofMinutes(it) } ?: penaltyTime?.duration

data class AppointmentEventTrigger(
  val triggeredAt: OffsetDateTime,
  val triggerType: AppointmentEventTriggerType,
  val triggeredBy: String,
) {
  companion object
}
