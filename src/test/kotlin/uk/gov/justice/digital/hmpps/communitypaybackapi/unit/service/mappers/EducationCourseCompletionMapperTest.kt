package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.mappers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentBehaviourDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentWorkQualityDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AttendanceDataDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAppointmentsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseCompletionMessage
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseCompletionStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseCourse
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCoursePerson
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.EducationCourseCompletionMapper
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class EducationCourseCompletionMapperTest {

  private val mapper = EducationCourseCompletionMapper()

  @Test
  fun `should map EducationCourseCompletionMessage to CreateAppointmentDto with correct values`() {
    val completionDateTime = LocalDateTime.of(2024, 1, 15, 14, 30)
    val totalTime = 120L // 2 hours
    val message = createTestMessage(
      completionDateTime = completionDateTime,
      totalTime = totalTime,
      crn = "X980484",
      courseName = "Maths Course",
    )

    val result = mapper.toCreateAppointmentDto(message)

    assertThat(result.crn).isEqualTo("X980484")
    assertThat(result.deliusEventNumber).isEqualTo(1)
    assertThat(result.allocationId).isNull()
    assertThat(result.date).isEqualTo(LocalDate.of(2024, 1, 15))
    assertThat(result.startTime).isEqualTo(LocalTime.of(12, 30)) // 14:30 - 2 hours
    assertThat(result.endTime).isEqualTo(LocalTime.of(14, 30))
    assertThat(result.pickUpLocationCode).isNull()
    assertThat(result.pickUpTime).isNull()
    assertThat(result.contactOutcomeCode).isEqualTo(ContactOutcomeEntity.ATTENDED_COMPLIED_OUTCOME_CODE)
    assertThat(result.supervisorOfficerCode).isNull()
    assertThat(result.notes).isEqualTo("Ete course completed: Maths Course")
    assertThat(result.alertActive).isNull()
    assertThat(result.sensitive).isNull()

    assertThat(result.attendanceData).isNotNull
    assertThat(result.attendanceData?.hiVisWorn).isFalse
    assertThat(result.attendanceData?.workedIntensively).isFalse
    assertThat(result.attendanceData?.penaltyMinutes).isNull()
    assertThat(result.attendanceData?.workQuality).isEqualTo(AppointmentWorkQualityDto.NOT_APPLICABLE)
    assertThat(result.attendanceData?.behaviour).isEqualTo(AppointmentBehaviourDto.NOT_APPLICABLE)
  }

  @Test
  fun `should generate UUID for appointment id`() {
    val message = createTestMessage()
    val result1 = mapper.toCreateAppointmentDto(message)
    val result2 = mapper.toCreateAppointmentDto(message)
    assertThat(result1.id).isNotNull
    assertThat(result2.id).isNotNull
    assertThat(result1.id).isNotEqualTo(result2.id) // Should generate different UUIDs each time
  }

  @Test
  fun `should handle different course durations correctly`() {
    val completionDateTime = LocalDateTime.of(2024, 1, 15, 10, 0)
    val totalTime = 30L // 30 minutes
    val message = createTestMessage(
      completionDateTime = completionDateTime,
      totalTime = totalTime,
    )
    val result = mapper.toCreateAppointmentDto(message)
    assertThat(result.startTime).isEqualTo(LocalTime.of(9, 30)) // 10:00 - 30 minutes
    assertThat(result.endTime).isEqualTo(LocalTime.of(10, 0))
  }

  @Test
  fun `should handle midnight crossing scenario`() {
    val completionDateTime = LocalDateTime.of(2024, 1, 15, 1, 0) // 1 AM
    val totalTime = 120L // 2 hours, would roll back to the previous day
    val message = createTestMessage(
      completionDateTime = completionDateTime,
      totalTime = totalTime,
    )

    val result = mapper.toCreateAppointmentDto(message)

    // Note: This test documents the behavior described in the TODO comment
    // The start time would be 23:00 on Jan 14, but date remains Jan 15
    // This would cause validation failure as mentioned in the code comment
    assertThat(result.startTime).isEqualTo(LocalTime.of(23, 0)) // 1:00 - 2 hours
    assertThat(result.endTime).isEqualTo(LocalTime.of(1, 0))
    assertThat(result.date).isEqualTo(LocalDate.of(2024, 1, 15)) // Date remains the completion date
  }

  @Test
  fun `should map to CreateAppointmentsDto with correct project code`() {
    val message = createTestMessage()
    val projectCode = "PROJ001"

    val result = mapper.toCreateAppointmentsDto(message, projectCode)

    assertThat(result).isInstanceOf(CreateAppointmentsDto::class.java)
    assertThat(result.projectCode).isEqualTo("PROJ001")
    assertThat(result.appointments).hasSize(1)

    val appointment = result.appointments.first()
    assertThat(appointment.crn).isEqualTo(message.person.crn)
    assertThat(appointment.notes).contains(message.course.courseName)
  }

  @Test
  fun `should include correct notes with course name`() {
    val courseName = "Advanced Programming"
    val message = createTestMessage(courseName = courseName)
    val result = mapper.toCreateAppointmentDto(message)
    assertThat(result.notes).isEqualTo("Ete course completed: $courseName")
  }

  @Test
  fun `should use correct contact outcome code`() {
    val message = createTestMessage()
    val result = mapper.toCreateAppointmentDto(message)
    assertThat(result.contactOutcomeCode).isEqualTo(ContactOutcomeEntity.ATTENDED_COMPLIED_OUTCOME_CODE)
  }

  @Test
  fun `companion object should create default attendance data`() {
    val attendanceData = EducationCourseCompletionMapper.DefaultEducationCourseCompletionAttendanceData.createAttendanceData()
    assertThat(attendanceData).isInstanceOf(AttendanceDataDto::class.java)
    assertThat(attendanceData.hiVisWorn).isFalse
    assertThat(attendanceData.workedIntensively).isFalse
    assertThat(attendanceData.penaltyMinutes).isNull()
    assertThat(attendanceData.workQuality).isEqualTo(AppointmentWorkQualityDto.NOT_APPLICABLE)
    assertThat(attendanceData.behaviour).isEqualTo(AppointmentBehaviourDto.NOT_APPLICABLE)
  }

  @Test
  fun `should handle different CRN values`() {
    val crn = "TEST12345"
    val message = createTestMessage(crn = crn)
    val result = mapper.toCreateAppointmentDto(message)
    assertThat(result.crn).isEqualTo(crn)
  }

  private fun createTestMessage(
    crn: String = "X980484",
    firstName: String = "John",
    lastName: String = "Doe",
    dateOfBirth: LocalDate = LocalDate.of(1990, 5, 20),
    region: String = "London",
    email: String = "john.doe@example.com",
    completionDateTime: LocalDateTime = LocalDateTime.of(2024, 1, 15, 14, 30),
    totalTime: Long = 120L,
    courseName: String = "Test Course",
    courseType: String = "Type A",
    provider: String = "Provider X",
    status: EducationCourseCompletionStatus = EducationCourseCompletionStatus.Completed,
    expectedMinutes: Int = 60,
    externalId: String = "EXT_ID",
  ): EducationCourseCompletionMessage = EducationCourseCompletionMessage(
    person = createDefaultPerson(
      crn = crn,
      firstName = firstName,
      lastName = lastName,
      dateOfBirth = dateOfBirth,
      region = region,
      email = email,
    ),
    course = createDefaultCourse(
      completionDateTime = completionDateTime,
      totalTime = totalTime,
      courseName = courseName,
      courseType = courseType,
      provider = provider,
      status = status,
      expectedMinutes = expectedMinutes,
    ),
    externalReference = externalId,
  )

  private fun createDefaultPerson(
    crn: String,
    firstName: String,
    lastName: String,
    dateOfBirth: LocalDate,
    region: String,
    email: String,
  ): EducationCoursePerson = EducationCoursePerson(
    crn = crn,
    firstName = firstName,
    lastName = lastName,
    dateOfBirth = dateOfBirth,
    region = region,
    email = email,
  )

  private fun createDefaultCourse(
    completionDateTime: LocalDateTime,
    totalTime: Long,
    courseName: String,
    courseType: String,
    provider: String,
    status: EducationCourseCompletionStatus,
    expectedMinutes: Int,
  ): EducationCourseCourse = EducationCourseCourse(
    completionDateTime = completionDateTime,
    totalTime = totalTime,
    courseName = courseName,
    courseType = courseType,
    provider = provider,
    status = status,
    expectedMinutes = expectedMinutes,
  )
}
