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
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseEventCompletionMessageStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.EducationCourseCompletionMapper
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@DisplayName("EducationCourseCompletionMapper")
class EducationCourseCompletionMapperTest {

  private lateinit var mapper: EducationCourseCompletionMapper

  private val crn = String.random(1).uppercase() + Int.random(0, 99999)
  private val projectCode = "PROJ123"

  @BeforeEach
  fun setUp() {
    mapper = EducationCourseCompletionMapper()
  }

  @Nested
  @DisplayName("toCreateAppointmentDto")
  inner class ToCreateAppointmentDto {
    @Test
    fun `should map all fields correctly`() {
      val entity = EteCourseCompletionEventEntity.valid()

      val result = mapper.toCreateAppointmentDto(entity, crn, projectCode)

      assertThat(result).isNotNull
      assertThat(result.id).isNotNull()
      assertThat(result.crn).isEqualTo(crn)
      assertThat(result.deliusEventNumber).isEqualTo(1)
      assertThat(result.allocationId).isNull()
      assertThat(result.date).isEqualTo(entity.completionDate)
      assertThat(result.notes).isEqualTo("Ete course completed: ${entity.courseName}")
      assertThat(result.contactOutcomeCode).isEqualTo(ContactOutcomeEntity.ATTENDED_COMPLIED_OUTCOME_CODE)
      assertThat(result.pickUpLocationCode).isNull()
      assertThat(result.pickUpTime).isNull()
      assertThat(result.supervisorOfficerCode).isNull()
      assertThat(result.alertActive).isNull()
      assertThat(result.sensitive).isNull()
    }

    @Test
    fun `should set start time to 9am`() {
      val entity = EteCourseCompletionEventEntity.valid()

      val result = mapper.toCreateAppointmentDto(entity, crn, projectCode)

      assertThat(result.startTime).isEqualTo(LocalTime.of(9, 0))
    }

    @Test
    fun `should calculate end time as start time plus total time minutes`() {
      val entity = EteCourseCompletionEventEntity.valid()

      val result = mapper.toCreateAppointmentDto(entity, crn, projectCode)

      assertThat(result.endTime).isEqualTo(LocalTime.of(9, 0).plusMinutes(entity.totalTimeMinutes))
    }

    @ParameterizedTest
    @ValueSource(longs = [30, 60, 120, 180, 240])
    fun `should handle various total time minutes`(totalTimeMinutes: Long) {
      val entity = EteCourseCompletionEventEntity.valid().copy(
        totalTimeMinutes = totalTimeMinutes,
      )

      val result = mapper.toCreateAppointmentDto(entity, crn, projectCode)

      assertThat(result.endTime).isEqualTo(LocalTime.of(9, 0).plusMinutes(totalTimeMinutes))
    }

    @Test
    fun `should generate unique UUID for each appointment`() {
      val entity1 = EteCourseCompletionEventEntity.valid()
      val entity2 = EteCourseCompletionEventEntity.valid()

      val result1 = mapper.toCreateAppointmentDto(entity1, crn, projectCode)
      val result2 = mapper.toCreateAppointmentDto(entity2, crn, projectCode)

      assertThat(result1.id).isNotEqualTo(result2.id)
      assertThat(result1.id).isInstanceOf(UUID::class.java)
    }

    @Test
    fun `should set attendance data with default values`() {
      val entity = EteCourseCompletionEventEntity.valid()

      val result = mapper.toCreateAppointmentDto(entity, crn, projectCode)

      assertThat(result.attendanceData).isNotNull
      assertThat(result.attendanceData?.hiVisWorn).isFalse()
      assertThat(result.attendanceData?.workedIntensively).isFalse()
      assertThat(result.attendanceData?.penaltyMinutes).isNull()
      assertThat(result.attendanceData?.workQuality).isEqualTo(AppointmentWorkQualityDto.NOT_APPLICABLE)
      assertThat(result.attendanceData?.behaviour).isEqualTo(AppointmentBehaviourDto.NOT_APPLICABLE)
    }

    @Test
    fun `should handle large total time minutes that roll into next day`() {
      val entity = EteCourseCompletionEventEntity.valid()

      val result = mapper.toCreateAppointmentDto(entity, crn, projectCode)

      assertThat(result.startTime).isEqualTo(LocalTime.of(9, 0))
      assertThat(result.endTime).isEqualTo(LocalTime.of(9, 0).plusMinutes(entity.totalTimeMinutes))
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
      val result = EducationCourseCompletionMapper.createAttendanceData()

      assertThat(result).isNotNull
      assertThat(result.hiVisWorn).isFalse()
      assertThat(result.workedIntensively).isFalse()
      assertThat(result.penaltyMinutes).isNull()
      assertThat(result.workQuality).isEqualTo(AppointmentWorkQualityDto.NOT_APPLICABLE)
      assertThat(result.behaviour).isEqualTo(AppointmentBehaviourDto.NOT_APPLICABLE)
    }

    @Test
    fun `createAttendanceData should return new instance each call`() {
      val result1 = EducationCourseCompletionMapper.createAttendanceData()
      val result2 = EducationCourseCompletionMapper.createAttendanceData()

      assertThat(result1).isNotSameAs(result2)
    }
  }
}
