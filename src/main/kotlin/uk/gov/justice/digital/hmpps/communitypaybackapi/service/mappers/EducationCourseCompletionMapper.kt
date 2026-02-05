package uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentBehaviourDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentWorkQualityDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AttendanceDataDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAppointmentsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseCompletionMessage
import java.time.LocalTime
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

  @Suppress("MagicNumber")
  fun toCreateAppointmentDto(message: EducationCourseCompletionMessage): CreateAppointmentDto {
    val attributes = message.messageAttributes
    val completionDate = attributes.completionDate
    val startTime = LocalTime.of(9, 0) // Temporary until decided - 9am as start time

    return CreateAppointmentDto(
      id = UUID.randomUUID(),
      crn = "X980484", // X980484 <--- Use for testing - Hardcoded for now, until we have a CRN assigning mechanism
      deliusEventNumber = 1, // This is not right, we need to find the correct event id
      allocationId = null,
      date = completionDate,
      // If this rolls time back into the previous day, this fails appointment creation validation
      // because start time is after end time
      startTime = startTime,
      endTime = startTime.plusMinutes(attributes.totalTimeMinutes),
      pickUpLocationCode = null,
      pickUpTime = null,
      contactOutcomeCode = ContactOutcomeEntity.ATTENDED_COMPLIED_OUTCOME_CODE,
      attendanceData = createAttendanceData(),
      supervisorOfficerCode = null,
      notes = "Ete course completed: ${attributes.courseName}",
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
