package uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentBehaviourDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentWorkQualityDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AttendanceDataDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.EteCourseCompletionEventDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntity
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class EducationCourseCompletionMapper {

  fun toCreateAppointmentDto(
    eteCourseCompletionEventEntity: EteCourseCompletionEventEntity,
    courseCompletionOutcome: CourseCompletionOutcomeDto,
  ): CreateAppointmentDto {
    val completionDate = eteCourseCompletionEventEntity.completionDate
    val startTime = LocalTime.of(9, 0)
    val minutesToCredit = courseCompletionOutcome.minutesToCredit

    val creditLimit = ChronoUnit.MINUTES.between(startTime, LocalTime.MIDNIGHT.minusMinutes(1))
    if (minutesToCredit > creditLimit) {
      error("Cannot credit more than $creditLimit minutes")
    }

    return CreateAppointmentDto(
      id = UUID.randomUUID(),
      crn = courseCompletionOutcome.crn,
      deliusEventNumber = courseCompletionOutcome.deliusEventNumber,
      allocationId = null,
      projectCode = courseCompletionOutcome.projectCode,
      date = completionDate,
      // If this rolls time back into the previous day, this fails appointment creation validation
      // because start time is after end time
      startTime = startTime,
      endTime = startTime.plusMinutes(minutesToCredit),
      pickUpLocationCode = null,
      pickUpLocationDescription = null,
      pickUpTime = null,
      contactOutcomeCode = courseCompletionOutcome.contactOutcomeCode,
      attendanceData = createAttendanceData(),
      supervisorOfficerCode = null,
      notes = "Ete course completed: ${eteCourseCompletionEventEntity.courseName}",
      alertActive = null,
      sensitive = null,
    )
  }

  companion object DefaultEducationCourseCompletionAttendanceData {
    fun createAttendanceData(): AttendanceDataDto = AttendanceDataDto(
      hiVisWorn = false,
      workedIntensively = false,
      penaltyTime = null,
      penaltyMinutes = null,
      workQuality = AppointmentWorkQualityDto.NOT_APPLICABLE,
      behaviour = AppointmentBehaviourDto.NOT_APPLICABLE,
    )
  }
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
