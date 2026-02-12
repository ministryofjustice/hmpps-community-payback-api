package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.mappers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentBehaviourDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentWorkQualityDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AttendanceDataDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseEventCompletionMessageStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.EducationCourseCompletionMapper
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@DisplayName("EducationCourseCompletionMapper")
class EducationCourseCompletionMapperTest {

  private lateinit var mapper: EducationCourseCompletionMapper

  @BeforeEach
  fun setUp() {
    mapper = EducationCourseCompletionMapper()
  }

  @Nested
  @DisplayName("toCreateAppointmentsDto")
  inner class ToCreateAppointmentsDto {
    @Test
    fun `should map entity to CreateAppointmentsDto with correct project code`() {
      val entity = createTestEntity()
      val projectCode = "TEST-PROJECT-001"

      val result = mapper.toCreateAppointmentsDto(entity, projectCode)

      assertThat(result).isNotNull
      assertThat(result.projectCode).isEqualTo(projectCode)
      assertThat(result.appointments).hasSize(1)
    }

    @Test
    fun `should map entity to CreateAppointmentsDto with single appointment`() {
      val entity = createTestEntity()
      val projectCode = "TEST-PROJECT-001"

      val result = mapper.toCreateAppointmentsDto(entity, projectCode)

      assertThat(result.appointments).hasSize(1)
      val appointment = result.appointments.first()
      assertThat(appointment).isNotNull
    }
  }

  @Nested
  @DisplayName("toCreateAppointmentDto")
  inner class ToCreateAppointmentDto {
    @Test
    fun `should map all fields correctly`() {
      val completionDate = LocalDate.of(2024, 1, 15)
      val totalTimeMinutes = 120L
      val courseName = "Health and Safety Level 2"
      val entity = createTestEntity(
        completionDate = completionDate,
        totalTimeMinutes = totalTimeMinutes,
        courseName = courseName,
      )

      val result = mapper.toCreateAppointmentDto(entity)

      assertThat(result).isNotNull
      assertThat(result.id).isNotNull()
      assertThat(result.crn).isEqualTo("X980484")
      assertThat(result.deliusEventNumber).isEqualTo(1)
      assertThat(result.allocationId).isNull()
      assertThat(result.date).isEqualTo(completionDate)
      assertThat(result.notes).isEqualTo("Ete course completed: $courseName")
      assertThat(result.contactOutcomeCode).isEqualTo(ContactOutcomeEntity.ATTENDED_COMPLIED_OUTCOME_CODE)
      assertThat(result.pickUpLocationCode).isNull()
      assertThat(result.pickUpTime).isNull()
      assertThat(result.supervisorOfficerCode).isNull()
      assertThat(result.alertActive).isNull()
      assertThat(result.sensitive).isNull()
    }

    @Test
    fun `should set start time to 9am`() {
      val entity = createTestEntity()

      val result = mapper.toCreateAppointmentDto(entity)

      assertThat(result.startTime).isEqualTo(LocalTime.of(9, 0))
    }

    @Test
    fun `should calculate end time as start time plus total time minutes`() {
      val totalTimeMinutes = 90L
      val entity = createTestEntity(totalTimeMinutes = totalTimeMinutes)

      val result = mapper.toCreateAppointmentDto(entity)

      assertThat(result.endTime).isEqualTo(LocalTime.of(9, 0).plusMinutes(totalTimeMinutes))
      assertThat(result.endTime).isEqualTo(LocalTime.of(10, 30))
    }

    @ParameterizedTest
    @ValueSource(longs = [30, 60, 120, 180, 240])
    fun `should handle various total time minutes`(totalTimeMinutes: Long) {
      val entity = createTestEntity(totalTimeMinutes = totalTimeMinutes)

      val result = mapper.toCreateAppointmentDto(entity)

      assertThat(result.endTime).isEqualTo(LocalTime.of(9, 0).plusMinutes(totalTimeMinutes))
    }

    @Test
    fun `should generate unique UUID for each appointment`() {
      val entity1 = createTestEntity()
      val entity2 = createTestEntity()

      val result1 = mapper.toCreateAppointmentDto(entity1)
      val result2 = mapper.toCreateAppointmentDto(entity2)

      assertThat(result1.id).isNotEqualTo(result2.id)
      assertThat(result1.id).isInstanceOf(UUID::class.java)
    }

    @Test
    fun `should set attendance data with default values`() {
      val entity = createTestEntity()

      val result = mapper.toCreateAppointmentDto(entity)

      assertThat(result.attendanceData).isNotNull
      assertThat(result.attendanceData?.hiVisWorn).isFalse()
      assertThat(result.attendanceData?.workedIntensively).isFalse()
      assertThat(result.attendanceData?.penaltyMinutes).isNull()
      assertThat(result.attendanceData?.workQuality).isEqualTo(AppointmentWorkQualityDto.NOT_APPLICABLE)
      assertThat(result.attendanceData?.behaviour).isEqualTo(AppointmentBehaviourDto.NOT_APPLICABLE)
    }

    @Test
    fun `should handle large total time minutes that roll into next day`() {
      val totalTimeMinutes = 960L
      val entity = createTestEntity(totalTimeMinutes = totalTimeMinutes)

      val result = mapper.toCreateAppointmentDto(entity)

      assertThat(result.startTime).isEqualTo(LocalTime.of(9, 0))
      assertThat(result.endTime).isEqualTo(LocalTime.of(9, 0).plusMinutes(totalTimeMinutes))
    }
  }

  @Nested
  @DisplayName("toDto")
  inner class ToDto {
    @Test
    fun `should map all entity fields to DTO`() {
      val id = UUID.randomUUID()
      val firstName = "John"
      val lastName = "Smith"
      val dateOfBirth = LocalDate.of(1990, 1, 1)
      val region = "London"
      val email = "john.smith@email.com"
      val courseName = "Digital Skills"
      val courseType = "ONLINE"
      val provider = "Skills for Life"
      val completionDate = LocalDate.of(2024, 2, 20)
      val status = EteCourseEventCompletionMessageStatus.COMPLETED
      val totalTimeMinutes = 120L
      val expectedTimeMinutes = 120L
      val attempts = 1
      val externalReference = "REF-123-ABC"

      val entity = EteCourseCompletionEventEntity(
        id = id,
        firstName = firstName,
        lastName = lastName,
        dateOfBirth = dateOfBirth,
        region = region,
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
      )

      val result = entity.toDto()

      assertThat(result.id).isEqualTo(id)
      assertThat(result.firstName).isEqualTo(firstName)
      assertThat(result.lastName).isEqualTo(lastName)
      assertThat(result.dateOfBirth).isEqualTo(dateOfBirth)
      assertThat(result.region).isEqualTo(region)
      assertThat(result.email).isEqualTo(email)
      assertThat(result.courseName).isEqualTo(courseName)
      assertThat(result.courseType).isEqualTo(courseType)
      assertThat(result.provider).isEqualTo(provider)
      assertThat(result.completionDate).isEqualTo(completionDate)
      assertThat(result.status).isEqualTo(status)
      assertThat(result.totalTimeMinutes).isEqualTo(totalTimeMinutes)
      assertThat(result.expectedTimeMinutes).isEqualTo(expectedTimeMinutes)
      assertThat(result.attempts).isEqualTo(attempts)
      assertThat(result.externalReference).isEqualTo(externalReference)
    }
  }

  @Nested
  @DisplayName("DefaultEducationCourseCompletionAttendanceData")
  inner class DefaultAttendanceData {
    @Test
    fun `createAttendanceData should return default values`() {
      val result = EducationCourseCompletionMapper.DefaultEducationCourseCompletionAttendanceData.createAttendanceData()

      assertThat(result).isNotNull
      assertThat(result.hiVisWorn).isFalse()
      assertThat(result.workedIntensively).isFalse()
      assertThat(result.penaltyMinutes).isNull()
      assertThat(result.workQuality).isEqualTo(AppointmentWorkQualityDto.NOT_APPLICABLE)
      assertThat(result.behaviour).isEqualTo(AppointmentBehaviourDto.NOT_APPLICABLE)
    }

    @Test
    fun `createAttendanceData should return new instance each call`() {
      val result1 = EducationCourseCompletionMapper.DefaultEducationCourseCompletionAttendanceData.createAttendanceData()
      val result2 = EducationCourseCompletionMapper.DefaultEducationCourseCompletionAttendanceData.createAttendanceData()

      assertThat(result1).isNotSameAs(result2)
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  inner class IntegrationTests {
    @Test
    fun `full mapping flow from entity to appointments dto`() {
      val entity = createTestEntity(
        completionDate = LocalDate.of(2024, 3, 10),
        totalTimeMinutes = 90L,
        courseName = "First Aid",
      )
      val projectCode = "PROJ-2024-001"

      val appointmentsDto = mapper.toCreateAppointmentsDto(entity, projectCode)
      val appointmentDto = appointmentsDto.appointments.first()
      val courseCompletionDto = entity.toDto()

      assertThat(appointmentsDto.projectCode).isEqualTo(projectCode)
      assertThat(appointmentDto.date).isEqualTo(LocalDate.of(2024, 3, 10))
      assertThat(appointmentDto.startTime).isEqualTo(LocalTime.of(9, 0))
      assertThat(appointmentDto.endTime).isEqualTo(LocalTime.of(10, 30))
      assertThat(appointmentDto.notes).isEqualTo("Ete course completed: First Aid")

      assertThat(appointmentDto.attendanceData).isEqualTo(createExpectedAttendanceData())

      assertThat(courseCompletionDto.courseName).isEqualTo("First Aid")
      assertThat(courseCompletionDto.totalTimeMinutes).isEqualTo(90L)
    }
  }

  private fun createTestEntity(
    id: UUID = UUID.randomUUID(),
    firstName: String = "Test",
    lastName: String = "User",
    dateOfBirth: LocalDate = LocalDate.of(2000, 1, 1),
    region: String = "Test Region",
    email: String = "test.user@example.com",
    courseName: String = "Test Course",
    courseType: String = "ONLINE",
    provider: String = "Test Provider",
    completionDate: LocalDate = LocalDate.now(),
    status: EteCourseEventCompletionMessageStatus = EteCourseEventCompletionMessageStatus.COMPLETED,
    totalTimeMinutes: Long = 60L,
    expectedTimeMinutes: Long = 60L,
    attempts: Int = 1,
    externalReference: String = "REF-123",
  ): EteCourseCompletionEventEntity = EteCourseCompletionEventEntity(
    id = id,
    firstName = firstName,
    lastName = lastName,
    dateOfBirth = dateOfBirth,
    region = region,
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
  )

  private fun createExpectedAttendanceData(): AttendanceDataDto = AttendanceDataDto(
    hiVisWorn = false,
    workedIntensively = false,
    penaltyTime = null,
    penaltyMinutes = null,
    workQuality = AppointmentWorkQualityDto.NOT_APPLICABLE,
    behaviour = AppointmentBehaviourDto.NOT_APPLICABLE,
  )
}
