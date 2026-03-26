package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.mappers

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentBehaviourDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentWorkQualityDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionCreditTimeDetailsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionResolutionDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.EteCourseCompletionEventStatusDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.CommunityCampusPduEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.CommunityCampusPduEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventResolutionEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionResolution
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.listener.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseCompletionMessage
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseCompletionStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseMessageAttributes
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.ContextService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.EteMappers
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

@ExtendWith(MockKExtension::class)
class EteMappersTest {

  @RelaxedMockK
  private lateinit var contextService: ContextService

  @RelaxedMockK
  private lateinit var contactOutcomeEntityRepository: ContactOutcomeEntityRepository

  @RelaxedMockK
  private lateinit var communityCampusPduEntityRepository: CommunityCampusPduEntityRepository

  @InjectMockKs
  private lateinit var mapper: EteMappers

  companion object {
    const val CONTACT_OUTCOME_CODE = "OUTCOME1"
    const val CRN = "CRN1234"
    const val PROJECT_CODE = "PROJ123"
    const val DELIUS_APPOINTMENT_ID = 5L
    const val DELIUS_EVENT_NUMBER = 52
  }

  @Nested
  inner class EntityToCreateAppointmentDto {

    val baselineCreditTimeDetailsDto = CourseCompletionCreditTimeDetailsDto.valid().copy(
      projectCode = PROJECT_CODE,
      deliusEventNumber = DELIUS_EVENT_NUMBER,
      contactOutcomeCode = CONTACT_OUTCOME_CODE,
    )

    val baselineCourseCompletionResolution = CourseCompletionResolutionDto.valid().copy(
      crn = CRN,
      creditTimeDetails = baselineCreditTimeDetailsDto,
    )

    @ParameterizedTest
    @CsvSource(
      nullValues = ["null"],
      value = ["true,true", "true,false", "false,true", "null,null"],
    )
    fun `should map all fields correctly`(
      sensitive: Boolean?,
      alertActive: Boolean?,
    ) {
      val result = mapper.toCreateAppointmentDto(
        baselineCourseCompletionResolution.copy(
          creditTimeDetails = baselineCourseCompletionResolution.creditTimeDetails!!.copy(
            minutesToCredit = 60L,
            notes = "the provided notes",
            sensitive = sensitive,
            alertActive = alertActive,
          ),
        ),
      )

      assertThat(result).isNotNull
      assertThat(result.id).isNotNull()
      assertThat(result.crn).isEqualTo(CRN)
      assertThat(result.deliusEventNumber).isEqualTo(DELIUS_EVENT_NUMBER)
      assertThat(result.allocationId).isNull()
      assertThat(result.date).isEqualTo(baselineCourseCompletionResolution.creditTimeDetails.date)
      assertThat(result.notes).isEqualTo("the provided notes")
      assertThat(result.contactOutcomeCode).isEqualTo(CONTACT_OUTCOME_CODE)
      assertThat(result.pickUpLocationCode).isNull()
      assertThat(result.pickUpTime).isNull()
      assertThat(result.supervisorOfficerCode).isNull()
      assertThat(result.alertActive).isEqualTo(alertActive)
      assertThat(result.sensitive).isEqualTo(sensitive)
    }

    @Test
    fun `should set start time to 9am`() {
      val result = mapper.toCreateAppointmentDto(
        baselineCourseCompletionResolution.copy(

          creditTimeDetails = CourseCompletionCreditTimeDetailsDto.valid().copy(
            minutesToCredit = 60L,
          ),
        ),
      )

      assertThat(result.startTime).isEqualTo(LocalTime.of(0, 0))
    }

    @ParameterizedTest
    @ValueSource(longs = [30, 60, 120, 180, 240])
    fun `should calculate end time as start time plus total time minutes`(
      minutesToCredit: Long,
    ) {
      val result = mapper.toCreateAppointmentDto(
        baselineCourseCompletionResolution.copy(

          creditTimeDetails = CourseCompletionCreditTimeDetailsDto.valid().copy(
            minutesToCredit = minutesToCredit,
          ),
        ),
      )

      assertThat(result.endTime).isEqualTo(LocalTime.of(0, 0).plusMinutes(minutesToCredit))
    }

    @Test
    fun `should set attendance data with default values`() {
      val result = mapper.toCreateAppointmentDto(
        baselineCourseCompletionResolution.copy(

          creditTimeDetails = CourseCompletionCreditTimeDetailsDto.valid().copy(
            minutesToCredit = 60L,
          ),
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
      assertThatThrownBy {
        mapper.toCreateAppointmentDto(
          baselineCourseCompletionResolution.copy(
            creditTimeDetails = CourseCompletionCreditTimeDetailsDto.valid().copy(
              minutesToCredit = 60L * 24,
            ),
          ),
        )
      }.hasMessage("Cannot credit more than 1439 minutes")
    }
  }

  @Nested
  inner class EntityToUpdateAppointmentDto {

    val baselineCourseCompletionOutcome = CourseCompletionResolutionDto.valid().copy(
      crn = CRN,
      creditTimeDetails = CourseCompletionCreditTimeDetailsDto.valid().copy(
        projectCode = PROJECT_CODE,
        deliusEventNumber = DELIUS_EVENT_NUMBER,
        contactOutcomeCode = CONTACT_OUTCOME_CODE,
      ),
    )

    val baselineExistingAppointment = AppointmentDto.valid().copy(
      date = baselineCourseCompletionOutcome.creditTimeDetails!!.date,
    )

    @ParameterizedTest
    @CsvSource(
      nullValues = ["null"],
      value = ["true,true", "true,false", "false,true", "null,null"],
    )
    fun `should map all fields correctly`(
      sensitive: Boolean?,
      alertActive: Boolean?,
    ) {
      val existingAppointment = baselineExistingAppointment.copy()

      val result = mapper.toUpdateAppointmentDto(
        courseCompletionResolution = baselineCourseCompletionOutcome.copy(

          creditTimeDetails = baselineCourseCompletionOutcome.creditTimeDetails!!.copy(
            minutesToCredit = 60L,
            notes = "the provided notes",
            sensitive = sensitive,
            alertActive = alertActive,
          ),
        ),
        existingAppointment = existingAppointment,
      )

      assertThat(result.deliusId).isEqualTo(existingAppointment.id)
      assertThat(result.deliusVersionToUpdate).isEqualTo(existingAppointment.version)
      assertThat(result.contactOutcomeCode).isEqualTo(CONTACT_OUTCOME_CODE)
      assertThat(result.enforcementData).isNull()
      assertThat(result.supervisorOfficerCode).isEqualTo(existingAppointment.supervisorOfficerCode)
      assertThat(result.notes).isEqualTo("the provided notes")
      assertThat(result.alertActive).isEqualTo(alertActive)
      assertThat(result.sensitive).isEqualTo(sensitive)
    }

    @Test
    fun `should set start time to first minute of the day`() {
      val existingAppointment = baselineExistingAppointment.copy()

      val result = mapper.toUpdateAppointmentDto(
        courseCompletionResolution = baselineCourseCompletionOutcome.copy(
          creditTimeDetails = baselineCourseCompletionOutcome.creditTimeDetails!!.copy(
            minutesToCredit = 60L,
          ),
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
      val existingAppointment = baselineExistingAppointment.copy()

      val result = mapper.toUpdateAppointmentDto(
        courseCompletionResolution = baselineCourseCompletionOutcome.copy(
          creditTimeDetails = baselineCourseCompletionOutcome.creditTimeDetails!!.copy(
            minutesToCredit = minutesToCredit,
          ),
        ),
        existingAppointment = existingAppointment,
      )

      assertThat(result.endTime).isEqualTo(LocalTime.of(0, 0).plusMinutes(minutesToCredit))
    }

    @Test
    fun `should error if crediting minutes that would roll into next day`() {
      assertThatThrownBy {
        val existingAppointment = baselineExistingAppointment.copy()

        mapper.toUpdateAppointmentDto(
          courseCompletionResolution = baselineCourseCompletionOutcome.copy(
            creditTimeDetails = baselineCourseCompletionOutcome.creditTimeDetails!!.copy(
              minutesToCredit = 60L * 24,
            ),
          ),
          existingAppointment = existingAppointment,
        )
      }.hasMessage("Cannot credit more than 1439 minutes")
    }

    @Test
    fun `error if attempting to change date`() {
      assertThatThrownBy {
        val existingAppointment = baselineExistingAppointment.copy()

        mapper.toUpdateAppointmentDto(
          courseCompletionResolution = baselineCourseCompletionOutcome.copy(
            creditTimeDetails = baselineCourseCompletionOutcome.creditTimeDetails!!.copy(
              date = baselineExistingAppointment.date.plusDays(1),
            ),
          ),
          existingAppointment = existingAppointment,
        )
      }.hasMessage("Changing an existing appointment's date is not currently supported")
    }
  }

  @Nested
  inner class EntityToCourseCompletionEventDto {

    @ParameterizedTest
    @CsvSource("true", "false")
    fun `should map all entity fields to DTO`(resolved: Boolean) {
      val id = UUID.randomUUID()
      val firstName = "John"
      val lastName = "Smith"
      val dateOfBirth = LocalDate.of(1990, 1, 1)
      val region = "London"
      val email = "john.smith@email.com"
      val courseName = "Digital Skills"
      val courseType = "ONLINE"
      val provider = "Skills for Life"
      val completionDateTime = OffsetDateTime.parse("2024-02-20T09:00:00Z")
      val status = EteCourseCompletionEventStatus.PASSED
      val totalTimeMinutes = 120L
      val expectedTimeMinutes = 120L
      val attempts = 1
      val externalReference = "REF-123-ABC"
      val receivedOn = OffsetDateTime.parse("2007-06-03T10:15:30+01:00")

      val entity = EteCourseCompletionEventEntity(
        id = id,
        firstName = firstName,
        lastName = lastName,
        dateOfBirth = dateOfBirth,
        region = region,
        pdu = CommunityCampusPduEntity.valid().copy(name = "test pdu"),
        office = "Office 1",
        email = email,
        courseName = courseName,
        courseType = courseType,
        provider = provider,
        completionDateTime = completionDateTime,
        status = status,
        totalTimeMinutes = totalTimeMinutes,
        expectedTimeMinutes = expectedTimeMinutes,
        attempts = attempts,
        externalReference = externalReference,
        receivedAt = receivedOn,
        createdAt = OffsetDateTime.now(),
        resolution = if (resolved) {
          EteCourseCompletionEventResolutionEntity.valid()
        } else {
          null
        },
      )

      val result = entity.toDto()

      assertThat(result.id).isEqualTo(id)
      assertThat(result.firstName).isEqualTo(firstName)
      assertThat(result.lastName).isEqualTo(lastName)
      assertThat(result.dateOfBirth).isEqualTo(dateOfBirth)
      assertThat(result.region).isEqualTo(region)
      assertThat(result.pdu.name).isEqualTo("test pdu")
      assertThat(result.office).isEqualTo("Office 1")
      assertThat(result.email).isEqualTo(email)
      assertThat(result.courseName).isEqualTo(courseName)
      assertThat(result.courseType).isEqualTo(courseType)
      assertThat(result.provider).isEqualTo(provider)
      assertThat(result.completionDateTime).isEqualTo(completionDateTime)
      assertThat(result.status).isEqualTo(EteCourseCompletionEventStatusDto.Passed)
      assertThat(result.totalTimeMinutes).isEqualTo(totalTimeMinutes)
      assertThat(result.expectedTimeMinutes).isEqualTo(expectedTimeMinutes)
      assertThat(result.attempts).isEqualTo(attempts)
      assertThat(result.externalReference).isEqualTo(externalReference)
      assertThat(result.importedOn).isEqualTo(LocalDateTime.parse("2007-06-03T10:15:30"))
      assertThat(result.resolved).isEqualTo(resolved)
    }
  }

  @Nested
  inner class DtoToResolutionEntity {

    val resolutionId: UUID = UUID.randomUUID()

    @Test
    fun `for course already completed`() {
      every { contextService.getUserName() } returns "jeff"

      val courseCompletionEvent = EteCourseCompletionEventEntity.valid()

      val result = mapper.toResolutionEntityForCourseAlreadyCompleted(
        id = resolutionId,
        courseCompletionEvent = courseCompletionEvent,
        courseCompletionResolution = CourseCompletionResolutionDto.valid().copy(
          crn = CRN,
          creditTimeDetails = null,
        ),
      )

      assertThat(result.id).isEqualTo(resolutionId)
      assertThat(result.eteCourseCompletionEvent).isEqualTo(courseCompletionEvent)
      assertThat(result.resolution).isEqualTo(EteCourseCompletionResolution.COURSE_ALREADY_COMPLETED_WITHIN_THRESHOLD)
      assertThat(result.createdByUsername).isEqualTo("jeff")
      assertThat(result.crn).isEqualTo(CRN)
      assertThat(result.deliusEventNumber).isNull()
      assertThat(result.deliusAppointmentId).isNull()
      assertThat(result.deliusAppointmentCreated).isNull()
      assertThat(result.projectCode).isNull()
      assertThat(result.minutesCredited).isNull()
      assertThat(result.contactOutcome).isNull()
    }

    @ParameterizedTest
    @CsvSource("true", "false")
    fun `for credit time`(appointmentCreated: Boolean) {
      every { contextService.getUserName() } returns "jeff"
      val contactOutcome = ContactOutcomeEntity.valid()
      every { contactOutcomeEntityRepository.findByCode(CONTACT_OUTCOME_CODE) } returns contactOutcome

      val courseCompletionEvent = EteCourseCompletionEventEntity.valid()

      val result = mapper.toResolutionEntityForCreditTime(
        id = resolutionId,
        courseCompletionEvent = courseCompletionEvent,
        courseCompletionResolution = CourseCompletionResolutionDto.valid().copy(
          crn = CRN,
          creditTimeDetails = CourseCompletionCreditTimeDetailsDto.valid().copy(
            deliusEventNumber = DELIUS_EVENT_NUMBER,
            projectCode = PROJECT_CODE,
            minutesToCredit = 97,
            contactOutcomeCode = CONTACT_OUTCOME_CODE,
            appointmentIdToUpdate = if (appointmentCreated) {
              null
            } else {
              DELIUS_APPOINTMENT_ID
            },
          ),
        ),
        deliusAppointmentId = DELIUS_APPOINTMENT_ID,
      )

      assertThat(result.id).isEqualTo(resolutionId)
      assertThat(result.eteCourseCompletionEvent).isEqualTo(courseCompletionEvent)
      assertThat(result.resolution).isEqualTo(EteCourseCompletionResolution.CREDIT_TIME)
      assertThat(result.createdByUsername).isEqualTo("jeff")
      assertThat(result.crn).isEqualTo(CRN)
      assertThat(result.deliusEventNumber).isEqualTo(DELIUS_EVENT_NUMBER)
      assertThat(result.deliusAppointmentId).isEqualTo(DELIUS_APPOINTMENT_ID)
      assertThat(result.deliusAppointmentCreated).isEqualTo(appointmentCreated)
      assertThat(result.projectCode).isEqualTo(PROJECT_CODE)
      assertThat(result.minutesCredited).isEqualTo(97)
      assertThat(result.contactOutcome).isEqualTo(contactOutcome)
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

  @Nested
  inner class EducationCourseCompletionMessageToEntity {

    @Test
    fun `error if PDU can't be found`() {
      every { communityCampusPduEntityRepository.findByNameIgnoreCase("invalid pdu name") } returns null

      assertThatThrownBy {
        mapper.toCourseCompletionEventEntity(
          EducationCourseCompletionMessage.valid().copy(
            messageAttributes = EducationCourseMessageAttributes.valid().copy(
              pdu = "invalid pdu name",
            ),
          ),
        )
      }.hasMessage("Cannot find PDU for name invalid pdu name")
    }

    @Test
    fun `map all fields`() {
      val pdu = CommunityCampusPduEntity.valid()
      every { communityCampusPduEntityRepository.findByNameIgnoreCase("The PDU name") } returns pdu

      val result = mapper.toCourseCompletionEventEntity(
        EducationCourseCompletionMessage.valid().copy(
          messageAttributes = EducationCourseMessageAttributes(
            externalReference = "EXT123",
            firstName = "John",
            lastName = "Doe",
            dateOfBirth = LocalDate.of(1990, 5, 15),
            region = "London",
            office = "The Office",
            pdu = " The PDU name ",
            email = "john.doe@example.com",
            courseName = "The course name",
            courseType = "Online",
            provider = "Training Provider Inc.",
            totalTimeMinutes = 70,
            expectedTimeMinutes = 120,
            status = EducationCourseCompletionStatus.Failed,
            completionDateTime = OffsetDateTime.parse("2026-01-01T10:00:00Z"),
            attempts = 5,
          ),
        ),
      )

      assertThat(result.firstName).isEqualTo("John")
      assertThat(result.lastName).isEqualTo("Doe")
      assertThat(result.dateOfBirth).isEqualTo(LocalDate.of(1990, 5, 15))
      assertThat(result.region).isEqualTo("London")
      assertThat(result.pdu).isEqualTo(pdu)
      assertThat(result.office).isEqualTo("The Office")
      assertThat(result.email).isEqualTo("john.doe@example.com")
      assertThat(result.courseName).isEqualTo("The course name")
      assertThat(result.courseType).isEqualTo("Online")
      assertThat(result.provider).isEqualTo("Training Provider Inc.")
      assertThat(result.totalTimeMinutes).isEqualTo(70)
      assertThat(result.expectedTimeMinutes).isEqualTo(120)
      assertThat(result.status).isEqualTo(EteCourseCompletionEventStatus.FAILED)
      assertThat(result.completionDateTime).isEqualTo("2026-01-01T10:00:00Z")
      assertThat(result.externalReference).isEqualTo("EXT123")
      assertThat(result.receivedAt).isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.SECONDS))
    }
  }
}
