package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.mappers

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentBehaviourDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentWorkQualityDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseEventCompletionMessageStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.EducationCourseCompletionMapper
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.UUID

@DisplayName("EducationCourseCompletionMapper")
class EducationCourseCompletionMapperTest {

  private lateinit var mapper: EducationCourseCompletionMapper

  companion object {
    const val CONTACT_OUTCOME_CODE = "OUTCOME1"
    const val CRN = "CRN1234"
    const val PROJECT_CODE = "PROJ123"
    const val DELIUS_EVENT_NUMBER = 52L
  }

  val baselineCourseCompletionOutcome = CourseCompletionOutcomeDto.valid().copy(
    crn = CRN,
    projectCode = PROJECT_CODE,
    deliusEventNumber = DELIUS_EVENT_NUMBER,
    contactOutcomeCode = CONTACT_OUTCOME_CODE,
  )

  @BeforeEach
  fun setUp() {
    mapper = EducationCourseCompletionMapper()
  }

  @Nested
  inner class EntityToCreateAppointmentDto {
    @Test
    fun `should map all fields correctly`() {
      val entity = EteCourseCompletionEventEntity.valid().copy(courseName = "the course name")

      val result = mapper.toCreateAppointmentDto(
        eteCourseCompletionEventEntity = entity,
        baselineCourseCompletionOutcome.copy(
          minutesToCredit = 60L,
        ),
      )

      assertThat(result).isNotNull
      assertThat(result.id).isNotNull()
      assertThat(result.crn).isEqualTo(CRN)
      assertThat(result.deliusEventNumber).isEqualTo(DELIUS_EVENT_NUMBER)
      assertThat(result.allocationId).isNull()
      assertThat(result.date).isEqualTo(entity.completionDate)
      assertThat(result.notes).isEqualTo("Ete course completed: the course name")
      assertThat(result.contactOutcomeCode).isEqualTo(CONTACT_OUTCOME_CODE)
      assertThat(result.pickUpLocationCode).isNull()
      assertThat(result.pickUpTime).isNull()
      assertThat(result.supervisorOfficerCode).isNull()
      assertThat(result.alertActive).isNull()
      assertThat(result.sensitive).isNull()
    }

    @Test
    fun `should set start time to 9am`() {
      val entity = EteCourseCompletionEventEntity.valid()

      val result = mapper.toCreateAppointmentDto(
        eteCourseCompletionEventEntity = entity,
        baselineCourseCompletionOutcome.copy(
          minutesToCredit = 60L,
        ),
      )

      assertThat(result.startTime).isEqualTo(LocalTime.of(0, 0))
    }

    @ParameterizedTest
    @ValueSource(longs = [30, 60, 120, 180, 240])
    fun `should calculate end time as start time plus total time minutes`(
      minutesToCredit: Long,
    ) {
      val entity = EteCourseCompletionEventEntity.valid()

      val result = mapper.toCreateAppointmentDto(
        eteCourseCompletionEventEntity = entity,
        baselineCourseCompletionOutcome.copy(
          minutesToCredit = minutesToCredit,
        ),
      )

      assertThat(result.endTime).isEqualTo(LocalTime.of(0, 0).plusMinutes(minutesToCredit))
    }

    @Test
    fun `should set attendance data with default values`() {
      val entity = EteCourseCompletionEventEntity.valid()

      val result = mapper.toCreateAppointmentDto(
        eteCourseCompletionEventEntity = entity,
        baselineCourseCompletionOutcome.copy(
          minutesToCredit = 60L,
        ),
      )

      assertThat(result.attendanceData).isNotNull
      assertThat(result.attendanceData?.hiVisWorn).isFalse()
      assertThat(result.attendanceData?.workedIntensively).isFalse()
      assertThat(result.attendanceData?.penaltyMinutes).isNull()
      assertThat(result.attendanceData?.workQuality).isEqualTo(AppointmentWorkQualityDto.NOT_APPLICABLE)
      assertThat(result.attendanceData?.behaviour).isEqualTo(AppointmentBehaviourDto.NOT_APPLICABLE)
    }

    @Test
    fun `should error if crediting minutes that would roll into next day`() {
      val entity = EteCourseCompletionEventEntity.valid()

      assertThatThrownBy {
        mapper.toCreateAppointmentDto(
          eteCourseCompletionEventEntity = entity,
          baselineCourseCompletionOutcome.copy(
            minutesToCredit = 60L * 24,
          ),
        )
      }.hasMessage("Cannot credit more than 1439 minutes")
    }
  }

  @Nested
  inner class EntityToUpdateAppointmentDto {

    @Test
    fun `should map all fields correctly`() {
      val entity = EteCourseCompletionEventEntity.valid().copy(courseName = "the course name")
      val existingAppointment = AppointmentDto.valid()

      val result = mapper.toUpdateAppointmentDto(
        eteCourseCompletionEventEntity = entity,
        courseCompletionOutcome = baselineCourseCompletionOutcome.copy(
          minutesToCredit = 60L,
        ),
        existingAppointment = existingAppointment,
      )

      assertThat(result.deliusId).isEqualTo(existingAppointment.id)
      assertThat(result.deliusVersionToUpdate).isEqualTo(existingAppointment.version)
      assertThat(result.contactOutcomeCode).isEqualTo(CONTACT_OUTCOME_CODE)
      assertThat(result.enforcementData).isNull()
      assertThat(result.supervisorOfficerCode).isEqualTo(existingAppointment.supervisorOfficerCode)
      assertThat(result.notes).isEqualTo("Ete course completed: the course name")
      assertThat(result.alertActive).isEqualTo(existingAppointment.alertActive)
      assertThat(result.sensitive).isEqualTo(existingAppointment.sensitive)
    }

    @Test
    fun `should set start time to first minute of the day`() {
      val entity = EteCourseCompletionEventEntity.valid()
      val existingAppointment = AppointmentDto.valid()

      val result = mapper.toUpdateAppointmentDto(
        eteCourseCompletionEventEntity = entity,
        courseCompletionOutcome = baselineCourseCompletionOutcome.copy(
          minutesToCredit = 60L,
        ),
        existingAppointment = existingAppointment,
      )

      assertThat(result.startTime).isEqualTo(LocalTime.of(0, 0))
    }

    @ParameterizedTest
    @ValueSource(longs = [30, 60, 120, 180, 240])
    fun `should calculate end time as start time plus total time minutes`(
      minutesToCredit: Long,
    ) {
      val entity = EteCourseCompletionEventEntity.valid()
      val existingAppointment = AppointmentDto.valid()

      val result = mapper.toUpdateAppointmentDto(
        eteCourseCompletionEventEntity = entity,
        courseCompletionOutcome = baselineCourseCompletionOutcome.copy(
          minutesToCredit = minutesToCredit,
        ),
        existingAppointment = existingAppointment,
      )

      assertThat(result.endTime).isEqualTo(LocalTime.of(0, 0).plusMinutes(minutesToCredit))
    }

    @Test
    fun `should error if crediting minutes that would roll into next day`() {
      assertThatThrownBy {
        val entity = EteCourseCompletionEventEntity.valid()
        val existingAppointment = AppointmentDto.valid()

        mapper.toUpdateAppointmentDto(
          eteCourseCompletionEventEntity = entity,
          courseCompletionOutcome = baselineCourseCompletionOutcome.copy(
            minutesToCredit = 60L * 24,
          ),
          existingAppointment = existingAppointment,
        )
      }.hasMessage("Cannot credit more than 1439 minutes")
    }
  }

  @Nested
  inner class EntityToEteCourseCompletionEventDto {
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
      val createdAt = OffsetDateTime.parse("2007-06-03T10:15:30+01:00")

      val entity = EteCourseCompletionEventEntity(
        id = id,
        firstName = firstName,
        lastName = lastName,
        dateOfBirth = dateOfBirth,
        region = region,
        office = "Office 1",
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
        createdAt = createdAt,
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
      assertThat(result.importedOn).isEqualTo(LocalDateTime.parse("2007-06-03T10:15:30"))
    }
  }

  @Nested
  @DisplayName("DefaultEducationCourseCompletionAttendanceData")
  inner class DefaultAttendanceData {
    @Test
    fun `createAttendanceData should return default values`() {
      val result = mapper.createAttendanceData()

      assertThat(result).isNotNull
      assertThat(result.hiVisWorn).isFalse()
      assertThat(result.workedIntensively).isFalse()
      assertThat(result.penaltyMinutes).isNull()
      assertThat(result.workQuality).isEqualTo(AppointmentWorkQualityDto.NOT_APPLICABLE)
      assertThat(result.behaviour).isEqualTo(AppointmentBehaviourDto.NOT_APPLICABLE)
    }

    @Test
    fun `createAttendanceData should return new instance each call`() {
      val result1 = mapper.createAttendanceData()
      val result2 = mapper.createAttendanceData()

      assertThat(result1).isNotSameAs(result2)
    }
  }
}
