package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.mappers

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.data.domain.PageImpl
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.toLocalTimeEuropeLondon
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentBehaviourDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentWorkQualityDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionCreditTimeDetailsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionDontCreditTimeDetailsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionResolutionDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.EteCourseCompletionEventStatusDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.CommunityCampusPduEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.CommunityCampusPduEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntityRepository.CourseFailureFilter
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntityRepository.ResolutionStatus
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
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.EteMappers.Companion.DEFAULT_APPOINTMENT_START_TIME
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
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

  @RelaxedMockK
  private lateinit var eteCourseCompletionEventEntityRepository: EteCourseCompletionEventEntityRepository

  @InjectMockKs
  private lateinit var mapper: EteMappers

  companion object {
    const val CONTACT_OUTCOME_CODE = "OUTCOME1"
    const val CRN = "CRN1234"
    const val PROJECT_CODE = "PROJ123"
    const val DELIUS_APPOINTMENT_ID = 5L
    const val DELIUS_EVENT_NUMBER = 52
  }

  fun setupGetAllAttemptsForCourseCompletionEvent(vararg courseCompletionEvents: EteCourseCompletionEventEntity) {
    val courseCompletionEvent = courseCompletionEvents.last()

    every {
      eteCourseCompletionEventEntityRepository.findAllWithFilters(
        providerCode = courseCompletionEvent.pdu.providerCode,
        pduId = null,
        officesCount = 0,
        offices = emptyList(),
        resolutionStatus = ResolutionStatus.UNRESOLVED,
        courseFailures = CourseFailureFilter.SHOW_ALL,
        externalReference = courseCompletionEvent.externalReference,
        fromDate = null,
        toDate = null,
        availableFromDate = null,
        availableToDate = null,
        pageable = any(),
      )
    } returns PageImpl(courseCompletionEvents.toList())
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

    val baselineCourseCompletionEvent = EteCourseCompletionEventEntity.valid().copy(
      courseName = "The course name",
      provider = "Provider1",
      completionDateTime = OffsetDateTime.parse("2021-05-04T12:15:00.000+01:00"),
      status = EteCourseCompletionEventStatus.PASSED,
    )

    @BeforeEach
    fun setup() {
      setupGetAllAttemptsForCourseCompletionEvent(baselineCourseCompletionEvent)
    }

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
        courseCompletionResolution = baselineCourseCompletionResolution.copy(
          creditTimeDetails = baselineCourseCompletionResolution.creditTimeDetails!!.copy(
            minutesToCredit = 60L,
            notes = "the provided notes",
            sensitive = sensitive,
            alertActive = alertActive,
          ),
        ),
        courseCompletionEvent = baselineCourseCompletionEvent,
      )

      assertThat(result).isNotNull
      assertThat(result.crn).isEqualTo(CRN)
      assertThat(result.deliusEventNumber).isEqualTo(DELIUS_EVENT_NUMBER)
      assertThat(result.allocationId).isNull()
      assertThat(result.date).isEqualTo(baselineCourseCompletionResolution.creditTimeDetails.date)
      assertThat(result.notes).isEqualTo(
        """
        |'The course name' was completed on Provider1 at 12:15 on 04/05/2021 and resulted in a pass on attempt 1
        |the provided notes
        """.trimMargin(),
      )
      assertThat(result.contactOutcomeCode).isEqualTo(CONTACT_OUTCOME_CODE)
      assertThat(result.pickUpLocationCode).isNull()
      assertThat(result.pickUpTime).isNull()
      assertThat(result.supervisorOfficerCode).isNull()
      assertThat(result.alertActive).isEqualTo(alertActive)
      assertThat(result.sensitive).isEqualTo(sensitive)
    }

    @Test
    fun `should set end time to recorded completion time`() {
      val result = mapper.toCreateAppointmentDto(
        courseCompletionResolution = baselineCourseCompletionResolution.copy(
          creditTimeDetails = CourseCompletionCreditTimeDetailsDto.valid().copy(
            minutesToCredit = 60L,
          ),
        ),
        courseCompletionEvent = baselineCourseCompletionEvent,
      )

      assertThat(result.endTime).isEqualTo(baselineCourseCompletionEvent.completionDateTime.toLocalTimeEuropeLondon())
    }

    @ParameterizedTest
    @ValueSource(longs = [30, 60, 90, 120, 180, 240])
    fun `should calculate start time as the number of credited minutes before the end time`(minutesToCredit: Long) {
      val result = mapper.toCreateAppointmentDto(
        courseCompletionResolution = baselineCourseCompletionResolution.copy(
          creditTimeDetails = CourseCompletionCreditTimeDetailsDto.valid().copy(
            minutesToCredit = minutesToCredit,
          ),
        ),
        courseCompletionEvent = baselineCourseCompletionEvent,
      )

      val expectedStartTime = baselineCourseCompletionEvent.completionDateTime.toLocalTimeEuropeLondon().minusMinutes(minutesToCredit)
      assertThat(result.startTime).isEqualTo(expectedStartTime)
    }

    @Test
    fun `should set attendance data with default values`() {
      val result = mapper.toCreateAppointmentDto(
        courseCompletionResolution = baselineCourseCompletionResolution.copy(
          creditTimeDetails = CourseCompletionCreditTimeDetailsDto.valid().copy(
            minutesToCredit = 60L,
          ),
        ),
        courseCompletionEvent = baselineCourseCompletionEvent,
      )

      assertThat(result.attendanceData).isNotNull
      assertThat(result.attendanceData?.hiVisWorn).isFalse()
      assertThat(result.attendanceData?.workedIntensively).isFalse()
      assertThat(result.attendanceData?.penaltyMinutes).isNull()
      assertThat(result.attendanceData?.workQuality).isEqualTo(AppointmentWorkQualityDto.NOT_APPLICABLE)
      assertThat(result.attendanceData?.behaviour).isEqualTo(AppointmentBehaviourDto.NOT_APPLICABLE)
    }

    @Test
    fun `should use start time as 00 01 and end time = start time + credit minutes, if crediting minutes would roll into previous day`() {
      val minutesToCredit = (60L * 12L) + 16L
      val result = mapper.toCreateAppointmentDto(
        courseCompletionResolution = baselineCourseCompletionResolution.copy(
          creditTimeDetails = CourseCompletionCreditTimeDetailsDto.valid().copy(
            minutesToCredit = (60L * 12L) + 16L,
          ),
        ),
        courseCompletionEvent = baselineCourseCompletionEvent,
      )
      assertThat(result.startTime).isEqualTo(DEFAULT_APPOINTMENT_START_TIME)
      assertThat(result.endTime).isEqualTo(DEFAULT_APPOINTMENT_START_TIME.plusMinutes(minutesToCredit))
    }

    @ParameterizedTest
    @CsvSource(
      value = [
        "2020-01-02T12:15:15.125+00:00,'The course name' was completed on Provider1 at 12:15 on 02/01/2020 and resulted in a pass on attempt 1",
        "2020-01-02T12:15:00.000Z,'The course name' was completed on Provider1 at 12:15 on 02/01/2020 and resulted in a pass on attempt 1",
        "2021-05-04T12:15:00.000+01:00,'The course name' was completed on Provider1 at 12:15 on 04/05/2021 and resulted in a pass on attempt 1",
        "2021-05-04T12:15:00.000Z,'The course name' was completed on Provider1 at 13:15 on 04/05/2021 and resulted in a pass on attempt 1",
      ],
      quoteCharacter = '"',
    )
    fun `ensure note date time is correct according to EuropeLondon timezone`(
      completionDateTime: OffsetDateTime,
      expectedNote: String,
    ) {
      val courseCompletionEvent = baselineCourseCompletionEvent.copy(
        completionDateTime = completionDateTime,
      )

      setupGetAllAttemptsForCourseCompletionEvent(courseCompletionEvent)

      val result = mapper.toCreateAppointmentDto(
        courseCompletionResolution = baselineCourseCompletionResolution.copy(
          creditTimeDetails = baselineCourseCompletionResolution.creditTimeDetails!!.copy(notes = null),
        ),
        courseCompletionEvent = courseCompletionEvent,
      )

      assertThat(result.notes).isEqualTo(expectedNote)
    }

    @Test
    fun `should use all course completion attempts to build notes`() {
      val courseCompletionAttempt1 = baselineCourseCompletionEvent.copy(
        completionDateTime = OffsetDateTime.of(2025, 11, 20, 13, 35, 0, 0, ZoneOffset.UTC),
        status = EteCourseCompletionEventStatus.FAILED,
        attempts = 1,
      )
      val courseCompletionAttempt2 = baselineCourseCompletionEvent.copy(
        completionDateTime = OffsetDateTime.of(2025, 11, 21, 11, 20, 0, 0, ZoneOffset.UTC),
        status = EteCourseCompletionEventStatus.FAILED,
        attempts = 2,
      )
      val courseCompletionAttempt3 = baselineCourseCompletionEvent.copy(
        completionDateTime = OffsetDateTime.of(2025, 11, 22, 14, 10, 0, 0, ZoneOffset.UTC),
        status = EteCourseCompletionEventStatus.PASSED,
        attempts = 3,
      )

      setupGetAllAttemptsForCourseCompletionEvent(courseCompletionAttempt1, courseCompletionAttempt2, courseCompletionAttempt3)

      val result = mapper.toCreateAppointmentDto(
        courseCompletionResolution = baselineCourseCompletionResolution.copy(
          creditTimeDetails = baselineCourseCompletionResolution.creditTimeDetails!!.copy(notes = "the provided notes"),
        ),
        courseCompletionEvent = courseCompletionAttempt3,
      )

      assertThat(result.notes).isEqualTo(
        """
        |'The course name' was completed on Provider1 at 13:35 on 20/11/2025 and resulted in a fail on attempt 1
        |'The course name' was completed on Provider1 at 11:20 on 21/11/2025 and resulted in a fail on attempt 2
        |'The course name' was completed on Provider1 at 14:10 on 22/11/2025 and resulted in a pass on attempt 3
        |the provided notes
        """.trimMargin(),
      )
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

    val baselineCourseCompletionEvent = EteCourseCompletionEventEntity.valid().copy(
      courseName = "The course name",
      provider = "Provider1",
      completionDateTime = OffsetDateTime.parse("2021-05-04T12:15:00.000+01:00"),
      status = EteCourseCompletionEventStatus.PASSED,
    )

    @BeforeEach
    fun setup() {
      setupGetAllAttemptsForCourseCompletionEvent(baselineCourseCompletionEvent)
    }

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
            date = LocalDate.of(2025, 1, 1),
          ),
        ),
        courseCompletionEvent = baselineCourseCompletionEvent,
        existingAppointment = existingAppointment,
      )

      assertThat(result.deliusId).isEqualTo(existingAppointment.id)
      assertThat(result.deliusVersionToUpdate).isEqualTo(existingAppointment.version)
      assertThat(result.date).isEqualTo(LocalDate.of(2025, 1, 1))
      assertThat(result.contactOutcomeCode).isEqualTo(CONTACT_OUTCOME_CODE)
      assertThat(result.supervisorOfficerCode).isEqualTo(existingAppointment.supervisorOfficerCode)
      assertThat(result.notes).isEqualTo(
        """
        |'The course name' was completed on Provider1 at 12:15 on 04/05/2021 and resulted in a pass on attempt 1
        |the provided notes
        """.trimMargin(),
      )
      assertThat(result.alertActive).isEqualTo(alertActive)
      assertThat(result.sensitive).isEqualTo(sensitive)
    }

    @Test
    fun `should set end time to recorded completion time`() {
      val existingAppointment = baselineExistingAppointment.copy()

      val result = mapper.toUpdateAppointmentDto(
        courseCompletionResolution = baselineCourseCompletionOutcome.copy(
          creditTimeDetails = baselineCourseCompletionOutcome.creditTimeDetails!!.copy(
            minutesToCredit = 60L,
          ),
        ),
        courseCompletionEvent = baselineCourseCompletionEvent,
        existingAppointment = existingAppointment,
      )

      assertThat(result.endTime).isEqualTo(baselineCourseCompletionEvent.completionDateTime.toLocalTimeEuropeLondon())
    }

    @ParameterizedTest
    @ValueSource(longs = [30, 60, 120, 180, 240])
    fun `should calculate start time as the number of credited minutes before the end time`(minutesToCredit: Long) {
      val existingAppointment = baselineExistingAppointment.copy()

      val result = mapper.toUpdateAppointmentDto(
        courseCompletionResolution = baselineCourseCompletionOutcome.copy(
          creditTimeDetails = baselineCourseCompletionOutcome.creditTimeDetails!!.copy(
            minutesToCredit = minutesToCredit,
          ),
        ),
        courseCompletionEvent = baselineCourseCompletionEvent,
        existingAppointment = existingAppointment,
      )

      val expectedStartTime = baselineCourseCompletionEvent.completionDateTime.toLocalTimeEuropeLondon().minusMinutes(minutesToCredit)
      assertThat(result.startTime).isEqualTo(expectedStartTime)
      assertThat(result.endTime).isEqualTo(baselineCourseCompletionEvent.completionDateTime.toLocalTimeEuropeLondon())
    }

    @Test
    fun `should use start time as 00 01 and end time = start time + credit minutes, if crediting minutes would roll into previous day`() {
      val minutesToCredit = (60L * 12L) + 16L
      val result = mapper.toUpdateAppointmentDto(
        courseCompletionResolution = baselineCourseCompletionOutcome.copy(
          creditTimeDetails = baselineCourseCompletionOutcome.creditTimeDetails!!.copy(
            minutesToCredit = minutesToCredit,
          ),
        ),
        courseCompletionEvent = baselineCourseCompletionEvent,
        existingAppointment = baselineExistingAppointment,
      )
      assertThat(result.startTime).isEqualTo(DEFAULT_APPOINTMENT_START_TIME)
      assertThat(result.endTime).isEqualTo(DEFAULT_APPOINTMENT_START_TIME.plusMinutes(minutesToCredit))
    }

    @ParameterizedTest
    @CsvSource(
      value = [
        "2020-01-02T12:15:00.000+00:00,'The course name' was completed on Provider1 at 12:15 on 02/01/2020 and resulted in a pass on attempt 1",
        "2020-01-02T12:15:00.000Z,'The course name' was completed on Provider1 at 12:15 on 02/01/2020 and resulted in a pass on attempt 1",
        "2021-05-04T12:15:00.000+01:00,'The course name' was completed on Provider1 at 12:15 on 04/05/2021 and resulted in a pass on attempt 1",
        "2021-05-04T12:15:00.000Z,'The course name' was completed on Provider1 at 13:15 on 04/05/2021 and resulted in a pass on attempt 1",
      ],
      quoteCharacter = '"',
    )
    fun `ensure note date time is correct according to EuropeLondon timezone`(
      completionDateTime: OffsetDateTime,
      expectedNote: String,
    ) {
      val courseCompletionEvent = baselineCourseCompletionEvent.copy(
        completionDateTime = completionDateTime,
      )

      setupGetAllAttemptsForCourseCompletionEvent(courseCompletionEvent)

      val result = mapper.toUpdateAppointmentDto(
        courseCompletionResolution = baselineCourseCompletionOutcome.copy(
          creditTimeDetails = baselineCourseCompletionOutcome.creditTimeDetails!!.copy(notes = null),
        ),
        courseCompletionEvent = courseCompletionEvent,
        existingAppointment = baselineExistingAppointment,
      )

      assertThat(result.notes).isEqualTo(expectedNote)
    }

    @Test
    fun `should use all course completion attempts to build notes`() {
      val courseCompletionAttempt1 = baselineCourseCompletionEvent.copy(
        completionDateTime = OffsetDateTime.of(2025, 11, 20, 13, 35, 0, 0, ZoneOffset.UTC),
        status = EteCourseCompletionEventStatus.FAILED,
        attempts = 1,
      )
      val courseCompletionAttempt2 = baselineCourseCompletionEvent.copy(
        completionDateTime = OffsetDateTime.of(2025, 11, 21, 11, 20, 0, 0, ZoneOffset.UTC),
        status = EteCourseCompletionEventStatus.FAILED,
        attempts = 2,
      )
      val courseCompletionAttempt3 = baselineCourseCompletionEvent.copy(
        completionDateTime = OffsetDateTime.of(2025, 11, 22, 14, 10, 0, 0, ZoneOffset.UTC),
        status = EteCourseCompletionEventStatus.PASSED,
        attempts = 3,
      )

      setupGetAllAttemptsForCourseCompletionEvent(courseCompletionAttempt1, courseCompletionAttempt2, courseCompletionAttempt3)

      val result = mapper.toUpdateAppointmentDto(
        courseCompletionResolution = baselineCourseCompletionOutcome.copy(
          creditTimeDetails = baselineCourseCompletionOutcome.creditTimeDetails!!.copy(notes = "the provided notes"),
        ),
        courseCompletionEvent = courseCompletionAttempt3,
        existingAppointment = baselineExistingAppointment,
      )

      assertThat(result.notes).isEqualTo(
        """
        |'The course name' was completed on Provider1 at 13:35 on 20/11/2025 and resulted in a fail on attempt 1
        |'The course name' was completed on Provider1 at 11:20 on 21/11/2025 and resulted in a fail on attempt 2
        |'The course name' was completed on Provider1 at 14:10 on 22/11/2025 and resulted in a pass on attempt 3
        |the provided notes
        """.trimMargin(),
      )
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
    fun `for don't credit time`() {
      every { contextService.getUserName() } returns "jeff"

      val courseCompletionEvent = EteCourseCompletionEventEntity.valid()

      val result = mapper.toResolutionEntityForDontCreditTime(
        id = resolutionId,
        courseCompletionEvent = courseCompletionEvent,
        courseCompletionResolution = CourseCompletionResolutionDto.valid().copy(
          crn = CRN,
          creditTimeDetails = null,
          dontCreditTimeDetails = CourseCompletionDontCreditTimeDetailsDto(
            notes = "some useful notes",
          ),
        ),
      )

      assertThat(result.id).isEqualTo(resolutionId)
      assertThat(result.eteCourseCompletionEvent).isEqualTo(courseCompletionEvent)
      assertThat(result.resolution).isEqualTo(EteCourseCompletionResolution.DONT_CREDIT_TIME)
      assertThat(result.createdByUsername).isEqualTo("jeff")
      assertThat(result.notes).isEqualTo("some useful notes")
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
            notes = "some useful notes",
          ),
        ),
        deliusAppointmentId = DELIUS_APPOINTMENT_ID,
      )

      assertThat(result.id).isEqualTo(resolutionId)
      assertThat(result.eteCourseCompletionEvent).isEqualTo(courseCompletionEvent)
      assertThat(result.resolution).isEqualTo(EteCourseCompletionResolution.CREDIT_TIME)
      assertThat(result.createdByUsername).isEqualTo("jeff")
      assertThat(result.notes).isEqualTo("some useful notes")
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
