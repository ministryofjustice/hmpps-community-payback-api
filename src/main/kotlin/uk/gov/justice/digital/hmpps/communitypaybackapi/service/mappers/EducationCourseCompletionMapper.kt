package uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentBehaviourDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentWorkQualityDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AttendanceDataDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAppointmentsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseCompletionMessage
import java.util.UUID

@Service
class EducationCourseCompletionMapper {

  fun toCreateAppointmentsDto(
    message: EducationCourseCompletionMessage,
    projectCode: String,
  ) = CreateAppointmentsDto(
    projectCode = projectCode,
    appointments = listOf(toCreateAppointmentDto(message)),
  )

  fun toCreateAppointmentDto(message: EducationCourseCompletionMessage): CreateAppointmentDto {
    val completionDateTime = message.course.completionDateTime
    val startTime = completionDateTime.toLocalTime().minusMinutes(message.course.totalTime)

    return CreateAppointmentDto(
      id = UUID.randomUUID(),
      crn = message.person.crn, // X980484 <--- Use for testing
      deliusEventNumber = 1, // This is not right, we need to find the correct event id
      allocationId = null,
      date = completionDateTime.toLocalDate(),
      // If this rolls time back into the previous day, this fails appointment creation validation
      // because start time is after end time
      startTime = startTime,
      endTime = completionDateTime.toLocalTime(),
      pickUpLocationCode = null,
      pickUpTime = null,
      contactOutcomeCode = ContactOutcomeEntity.ATTENDED_COMPLIED_OUTCOME_CODE,
      attendanceData = createAttendanceData(),
      supervisorOfficerCode = null,
      notes = "Ete course completed: ${message.course.courseName}",
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
