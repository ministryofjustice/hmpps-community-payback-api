package uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentBehaviourDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentWorkQualityDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AttendanceDataDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.EteCourseCompletionEventDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntity
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class EteMappers {

  companion object {
    val APPOINTMENT_START_TIME: LocalTime = LocalTime.of(0, 0)
  }

  fun toCreateAppointmentDto(
    eteCourseCompletionEventEntity: EteCourseCompletionEventEntity,
    courseCompletionOutcome: CourseCompletionOutcomeDto,
  ): CreateAppointmentDto {
    val completionDate = eteCourseCompletionEventEntity.completionDate

    return CreateAppointmentDto(
      id = UUID.randomUUID(),
      crn = courseCompletionOutcome.crn,
      deliusEventNumber = courseCompletionOutcome.deliusEventNumber,
      allocationId = null,
      projectCode = courseCompletionOutcome.projectCode,
      date = completionDate,
      startTime = APPOINTMENT_START_TIME,
      endTime = calculateEndTime(courseCompletionOutcome.minutesToCredit),
      pickUpLocationCode = null,
      pickUpLocationDescription = null,
      pickUpTime = null,
      contactOutcomeCode = courseCompletionOutcome.contactOutcomeCode,
      attendanceData = createAttendanceData(),
      supervisorOfficerCode = null,
      notes = buildNote(eteCourseCompletionEventEntity),
      alertActive = null,
      sensitive = null,
    )
  }

  fun toUpdateAppointmentDto(
    eteCourseCompletionEventEntity: EteCourseCompletionEventEntity,
    courseCompletionOutcome: CourseCompletionOutcomeDto,
    existingAppointment: AppointmentDto,
  ) = UpdateAppointmentOutcomeDto(
    deliusId = existingAppointment.id,
    deliusVersionToUpdate = existingAppointment.version,
    startTime = APPOINTMENT_START_TIME,
    endTime = calculateEndTime(courseCompletionOutcome.minutesToCredit),
    contactOutcomeCode = courseCompletionOutcome.contactOutcomeCode,
    attendanceData = createAttendanceData(),
    enforcementData = null,
    supervisorOfficerCode = existingAppointment.supervisorOfficerCode,
    notes = buildNote(eteCourseCompletionEventEntity),
    alertActive = existingAppointment.alertActive,
    sensitive = existingAppointment.sensitive,
  )

  private fun calculateEndTime(
    minutesToCredit: Long,
  ): LocalTime {
    val creditLimit = ChronoUnit.MINUTES.between(APPOINTMENT_START_TIME, LocalTime.MIDNIGHT.minusMinutes(1))
    if (minutesToCredit > creditLimit) {
      error("Cannot credit more than $creditLimit minutes")
    }

    return APPOINTMENT_START_TIME.plusMinutes(minutesToCredit)
  }

  private fun buildNote(eteCourseCompletionEventEntity: EteCourseCompletionEventEntity) = "Ete course completed: ${eteCourseCompletionEventEntity.courseName}"

  fun createAttendanceData() = AttendanceDataDto(
    hiVisWorn = false,
    workedIntensively = false,
    penaltyTime = null,
    penaltyMinutes = null,
    workQuality = AppointmentWorkQualityDto.NOT_APPLICABLE,
    behaviour = AppointmentBehaviourDto.NOT_APPLICABLE,
  )
}

fun EteCourseCompletionEventEntity.toDto() = EteCourseCompletionEventDto(
  id = id,
  firstName = firstName,
  lastName = lastName,
  dateOfBirth = dateOfBirth,
  region = region,
  office = office,
  email = email,
  courseName = courseName,
  courseType = courseType,
  provider = provider,
  completionDate = completionDate,
  status = status,
  totalTimeMinutes = totalTimeMinutes,
  expectedTimeMinutes = expectedTimeMinutes,
  attempts = attempts,
  externalReference = externalReference,
  importedOn = createdAt.toLocalDateTime(),
)
