package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.HourMinuteDuration
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.validation.ValidationResultItem
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AttendanceDataDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.PickUpLocationDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectAvailabilityDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectTypeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectTypeGroupDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SchedulingDayOfWeekDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UnpaidWorkDetailsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UnpaidWorkDetailsIdDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentCalculationService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentValidationService2.AppointmentValidationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentValidationService2.FindResult
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.CreateAppointmentValidationService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.OffenderService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.ProjectService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.ProviderService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.TeamId
import uk.gov.justice.digital.hmpps.communitypaybackapi.unit.common.validation.hasErrors
import uk.gov.justice.digital.hmpps.communitypaybackapi.unit.common.validation.hasNoErrors
import uk.gov.justice.digital.hmpps.communitypaybackapi.unit.common.validation.hasNoWarnings
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

@ExtendWith(MockKExtension::class)
class CreateAppointmentValidationServiceTest {

  @MockK
  lateinit var contactOutcomeEntityRepository: ContactOutcomeEntityRepository

  @MockK
  lateinit var offenderService: OffenderService

  @MockK
  lateinit var projectService: ProjectService

  @MockK
  lateinit var appointmentCalculationService: AppointmentCalculationService

  @MockK
  lateinit var providerService: ProviderService

  @InjectMockKs
  lateinit var service: CreateAppointmentValidationService

  companion object {
    const val CRN = "CRN123"
    const val EVENT_NUMBER = 1234321
    const val OUTCOME_CODE = "OUTCOME1"
    const val PICK_UP_LOCATION_CODE = "PICKUP1"
    const val PROJECT_CODE = "PROJ123"
    const val PROVIDER_CODE = "PROV1"
    const val TEAM_CODE = "TEAM1"
    val UPW_DETAILS_ID = UnpaidWorkDetailsIdDto(CRN, EVENT_NUMBER)
  }

  val baselineCreate = CreateAppointmentDto.valid().copy(
    crn = CRN,
    date = LocalDate.of(2025, 1, 1),
    deliusEventNumber = EVENT_NUMBER,
    projectCode = PROJECT_CODE,
    contactOutcomeCode = OUTCOME_CODE,
    startTime = LocalTime.MIN,
    endTime = LocalTime.MAX,
    pickUpLocationCode = PICK_UP_LOCATION_CODE,
  )
  val baselineOutcome = ContactOutcomeEntity.valid().copy(code = OUTCOME_CODE)
  val baselinePickUpLocation = PickUpLocationDto.valid()
  val baselineProject = ProjectDto.valid().copy(
    actualEndDateExclusive = null,
    availability = SchedulingDayOfWeekDto.entries.map { dayOfWeek ->
      ProjectAvailabilityDto.valid().copy(dayOfWeek = dayOfWeek)
    },
    projectType = ProjectTypeDto.valid().copy(group = ProjectTypeGroupDto.INDIVIDUAL),
    providerCode = PROVIDER_CODE,
    teamCode = TEAM_CODE,
  )
  val baselineUnpaidWorkDetails = UnpaidWorkDetailsDto.valid().copy(
    eventNumber = EVENT_NUMBER,
    sentenceDate = baselineCreate.date,
    requiredMinutes = 3600,
    completedMinutes = 1200,
    completedEteMinutes = 1200,
    adjustments = 0,
  )

  @BeforeEach
  fun setupBaselineMockResponses() {
    every { projectService.getProject(PROJECT_CODE) } returns baselineProject
    every { contactOutcomeEntityRepository.findByCode(baselineOutcome.code) } returns baselineOutcome
    every { providerService.getPickupLocation(TeamId(PROVIDER_CODE, TEAM_CODE), PICK_UP_LOCATION_CODE) } returns baselinePickUpLocation
    every { offenderService.getUnpaidWorkDetails(UPW_DETAILS_ID) } returns baselineUnpaidWorkDetails
    every { appointmentCalculationService.minutesToCredit(any(), any(), any(), any()) } returns Duration.ofMinutes(60)
  }

  @Nested
  inner class Success {

    @Test
    fun `baseline request passes`() {
      val ctx = AppointmentValidationContext.Create()
      val result = service.validate(baselineCreate, ctx)

      assertThat(result).hasNoErrors()
      assertThat(result).hasNoWarnings()

      assertThat(ctx.project).isEqualTo(baselineProject)
      assertThat(ctx.pickUpLocation).isEqualTo(FindResult.Found(baselinePickUpLocation))
      assertThat(ctx.contactOutcome).isEqualTo(FindResult.Found(baselineOutcome))
      assertThat(ctx.unpaidWorkDetails).isEqualTo(baselineUnpaidWorkDetails)
      assertThat(ctx.timeToCredit).isEqualTo(Duration.ofMinutes(60))
    }

    @Test
    fun `minutes to credit correctly calculated`() {
      every {
        appointmentCalculationService.minutesToCredit(
          contactOutcome = baselineOutcome,
          startTime = baselineCreate.startTime,
          endTime = baselineCreate.endTime,
          penaltyMinutes = Duration.ofMinutes(55),
        )
      } returns Duration.ofMinutes(125)

      val create = baselineCreate.copy(
        attendanceData = AttendanceDataDto.valid().copy(penaltyMinutes = 55L),
      )

      val ctx = AppointmentValidationContext.Create()
      val result = service.validate(create, ctx)

      assertThat(result).hasNoErrors()
      assertThat(result).hasNoWarnings()

      assertThat(ctx.project).isEqualTo(baselineProject)
      assertThat(ctx.pickUpLocation).isEqualTo(FindResult.Found(baselinePickUpLocation))
      assertThat(ctx.contactOutcome).isEqualTo(FindResult.Found(baselineOutcome))
      assertThat(ctx.unpaidWorkDetails).isEqualTo(baselineUnpaidWorkDetails)
      assertThat(ctx.timeToCredit).isEqualTo(Duration.ofMinutes(125))
    }
  }

  @Nested
  inner class Date {

    @Test
    fun `ok if date is before project end date`() {
      every { projectService.getProject(PROJECT_CODE) } returns baselineProject.copy(
        actualEndDateExclusive = LocalDate.of(2030, 5, 4),
      )

      val result = service.validate(
        baselineCreate.copy(
          date = LocalDate.of(2030, 5, 3),
        ),
        AppointmentValidationContext.Create(),
      )

      assertThat(result).hasNoErrors()
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `has error if date is on project end date`() {
      every { projectService.getProject(PROJECT_CODE) } returns baselineProject.copy(
        actualEndDateExclusive = LocalDate.of(2030, 5, 4),
      )

      val expectedErrors = listOf(
        ValidationResultItem(
          "$.date",
          "APPOINTMENT_DATE_IS_NOT_BEFORE_END_OF_PROJECT",
          mapOf(
            "date" to LocalDate.of(2030, 5, 4),
            "projectEndDateExclusive" to LocalDate.of(2030, 5, 4),
          ),
        ),
      )

      val result = service.validate(
        baselineCreate.copy(
          date = LocalDate.of(2030, 5, 4),
        ),
        AppointmentValidationContext.Create(),
      )

      assertThat(result).hasErrors(expectedErrors)
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `has error if date is after project end date`() {
      every { projectService.getProject(PROJECT_CODE) } returns baselineProject.copy(
        actualEndDateExclusive = LocalDate.of(2030, 5, 4),
      )

      val expectedErrors = listOf(
        ValidationResultItem(
          "$.date",
          "APPOINTMENT_DATE_IS_NOT_BEFORE_END_OF_PROJECT",
          mapOf(
            "date" to LocalDate.of(2030, 5, 5),
            "projectEndDateExclusive" to LocalDate.of(2030, 5, 4),
          ),
        ),
      )

      val result = service.validate(
        baselineCreate.copy(
          date = LocalDate.of(2030, 5, 5),
        ),
        AppointmentValidationContext.Create(),
      )

      assertThat(result).hasErrors(expectedErrors)
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `has error if date is before sentencing date`() {
      every { offenderService.getUnpaidWorkDetails(UPW_DETAILS_ID) } returns UnpaidWorkDetailsDto.valid().copy(
        eventNumber = EVENT_NUMBER,
        sentenceDate = LocalDate.of(2025, 1, 2),
      )

      val expectedErrors = listOf(
        ValidationResultItem(
          "$.date",
          "APPOINTMENT_DATE_IS_BEFORE_SENTENCE_DATE",
          mapOf(
            "date" to LocalDate.of(2025, 1, 1),
            "sentenceDate" to LocalDate.of(2025, 1, 2),
          ),
        ),
      )

      val result = service.validate(
        baselineCreate.copy(
          date = LocalDate.of(2025, 1, 1),
        ),
        AppointmentValidationContext.Create(),
      )

      assertThat(result).hasErrors(expectedErrors)
      assertThat(result).hasNoWarnings()
    }
  }

  @Nested
  inner class Availability {

    @Test
    fun `has error if project isn't available on requested day of week`() {
      every { projectService.getProject(PROJECT_CODE) } returns baselineProject.copy(
        availability = SchedulingDayOfWeekDto.entries.filter { it != SchedulingDayOfWeekDto.WEDNESDAY }.map { dayOfWeek ->
          ProjectAvailabilityDto.valid().copy(dayOfWeek = dayOfWeek)
        },
      )

      val expectedErrors = listOf(
        ValidationResultItem(
          "$.date",
          "PROJECT_NOT_AVAILABLE_ON_REQUESTED_DAY_OF_WEEK",
          mapOf(
            "requestedDayOfWeek" to DayOfWeek.WEDNESDAY,
            "availableDays" to listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
          ),
        ),
      )

      val result = service.validate(
        baselineCreate.copy(
          // Wednesday
          date = LocalDate.of(2026, 2, 25),
        ),
        AppointmentValidationContext.Create(),
      )

      assertThat(result).hasErrors(expectedErrors)
      assertThat(result).hasNoWarnings()
    }
  }

  @Nested
  inner class ContactOutcome {

    @Test
    fun `has error when contact outcome not found`() {
      every { contactOutcomeEntityRepository.findByCode(OUTCOME_CODE) } returns null

      val expectedErrors = listOf(
        ValidationResultItem(
          "$.contactOutcomeCode",
          "UNKNOWN_CONTACT_OUTCOME",
          mapOf(
            "code" to OUTCOME_CODE,
          ),
        ),
      )

      val result = service.validate(baselineCreate, AppointmentValidationContext.Create())

      assertThat(result).hasErrors(expectedErrors)
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `if appointment ended in the past, an outcome is mandatory`() {
      val outcome = baselineOutcome.copy(attended = true, enforceable = false)
      every { contactOutcomeEntityRepository.findByCode(OUTCOME_CODE) } returns outcome

      val expectedErrors = listOf(
        ValidationResultItem(
          "$.contactOutcomeCode",
          "PAST_APPOINTMENT_REQUIRES_CONTACT_OUTCOME",
          mapOf(),
        ),
      )

      val result = service.validate(
        baselineCreate.copy(
          date = LocalDate.now(),
          startTime = LocalTime.now().minusMinutes(2),
          endTime = LocalTime.now().minusMinutes(1),
          contactOutcomeCode = null,
        ),
        AppointmentValidationContext.Create(),
      )

      assertThat(result).hasErrors(expectedErrors)
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `if appointment is current or in the past, attendance outcome can be recorded`() {
      val outcome = baselineOutcome.copy(attended = true, enforceable = false)
      every { contactOutcomeEntityRepository.findByCode(OUTCOME_CODE) } returns outcome

      val result = service.validate(
        baselineCreate.copy(
          date = LocalDate.now(),
          startTime = LocalTime.now().minusMinutes(1),
        ),
        AppointmentValidationContext.Create(),
      )

      assertThat(result).hasNoErrors()
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `if appointment is current or in the past, enforceable outcome can be recorded`() {
      val outcome = baselineOutcome.copy(attended = false, enforceable = true)
      every { contactOutcomeEntityRepository.findByCode(OUTCOME_CODE) } returns outcome

      val result = service.validate(
        baselineCreate.copy(
          date = LocalDate.now(),
          startTime = LocalTime.now().minusMinutes(1),
        ),
        AppointmentValidationContext.Create(),
      )

      assertThat(result).hasNoErrors()
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `if appointment is current or in the past, non enforceable outcome can be recorded`() {
      val outcome = baselineOutcome.copy(attended = false, enforceable = false)
      every { contactOutcomeEntityRepository.findByCode(OUTCOME_CODE) } returns outcome

      val result = service.validate(
        baselineCreate.copy(
          date = LocalDate.now(),
          startTime = LocalTime.now().minusMinutes(1),
        ),
        AppointmentValidationContext.Create(),
      )

      assertThat(result).hasNoErrors()
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `if appointment is in future, attendance outcome can't be recorded`() {
      val outcome = baselineOutcome.copy(attended = true, enforceable = false)
      every { contactOutcomeEntityRepository.findByCode(OUTCOME_CODE) } returns outcome

      val expectedErrors = listOf(
        ValidationResultItem(
          "$.contactOutcomeCode",
          "FUTURE_APPOINTMENT_ONLY_ALLOWS_ACCEPTABLE_ABSENCE_OUTCOMES",
          mapOf(),
        ),
      )

      val result = service.validate(
        baselineCreate.copy(
          date = LocalDate.now().plusDays(1),
          startTime = LocalTime.NOON,
        ),
        AppointmentValidationContext.Create(),
      )

      assertThat(result).hasErrors(expectedErrors)
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `if appointment is in future, enforceable outcome can't be recorded`() {
      val outcome = baselineOutcome.copy(attended = false, enforceable = true)
      every { contactOutcomeEntityRepository.findByCode(outcome.code) } returns outcome

      val expectedErrors = listOf(
        ValidationResultItem(
          "$.contactOutcomeCode",
          "FUTURE_APPOINTMENT_ONLY_ALLOWS_ACCEPTABLE_ABSENCE_OUTCOMES",
          mapOf(),
        ),
      )

      val result = service.validate(
        baselineCreate.copy(
          date = LocalDate.now().plusDays(1),
          startTime = LocalTime.NOON,
        ),
        AppointmentValidationContext.Create(),
      )

      assertThat(result).hasErrors(expectedErrors)
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `if appointment is in future, non-enforceable absence can be recorded`() {
      val outcome = baselineOutcome.copy(attended = false, enforceable = false)
      every { contactOutcomeEntityRepository.findByCode(outcome.code) } returns outcome

      val result = service.validate(
        baselineCreate.copy(
          date = LocalDate.now().plusDays(1),
          startTime = LocalTime.NOON,
        ),
        AppointmentValidationContext.Create(),
      )

      assertThat(result).hasNoErrors()
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `if outcome attended is false, attendance data isn't required`() {
      val outcome = baselineOutcome.copy(attended = false, enforceable = false)
      every { contactOutcomeEntityRepository.findByCode(outcome.code) } returns outcome

      val result = service.validate(baselineCreate.copy(date = LocalDate.now()), AppointmentValidationContext.Create())

      assertThat(result).hasNoErrors()
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `if outcome attended is true and attendance data isn't provided, has error`() {
      val outcome = baselineOutcome.copy(attended = true, enforceable = false)
      every { contactOutcomeEntityRepository.findByCode(outcome.code) } returns outcome

      val expectedErrors = listOf(
        ValidationResultItem(
          "$.attendanceData",
          "ATTENDED_CONTACT_OUTCOME_REQUIRES_ATTENDANCE_DATA",
          mapOf(),
        ),
      )

      val result = service.validate(
        baselineCreate.copy(
          date = LocalDate.now(),
          attendanceData = null,
        ),
        AppointmentValidationContext.Create(),
      )

      assertThat(result).hasErrors(expectedErrors)
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `if outcome attended is true and attendance data is provided, is success`() {
      val outcome = baselineOutcome.copy(attended = true, enforceable = false)
      every { contactOutcomeEntityRepository.findByCode(outcome.code) } returns outcome

      val result = service.validate(
        baselineCreate.copy(
          date = LocalDate.now(),
          attendanceData = AttendanceDataDto.valid(),
        ),
        AppointmentValidationContext.Create(),
      )

      assertThat(result).hasNoErrors()
      assertThat(result).hasNoWarnings()
    }
  }

  @Nested
  inner class StartAndEndTime {

    @BeforeEach
    fun setupOutcome() {
      every { contactOutcomeEntityRepository.findByCode(baselineOutcome.code) } returns baselineOutcome
    }

    @Test
    fun `if end time same as start time, has error`() {
      val expectedErrors = listOf(
        ValidationResultItem(
          "$.endTime",
          "END_TIME_NOT_AFTER_START_TIME",
          mapOf(
            "startTime" to LocalTime.of(10, 0),
            "endTime" to LocalTime.of(10, 0),
          ),
        ),
      )

      val result = service.validate(
        baselineCreate.copy(
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(10, 0),
        ),
        AppointmentValidationContext.Create(),
      )

      assertThat(result).hasErrors(expectedErrors)
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `if end time after start time, do nothing`() {
      val result = service.validate(
        baselineCreate.copy(
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(10, 1),
        ),
        AppointmentValidationContext.Create(),
      )

      assertThat(result).hasNoErrors()
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `if end time before start time, has error`() {
      val expectedErrors = listOf(
        ValidationResultItem(
          "$.endTime",
          "END_TIME_NOT_AFTER_START_TIME",
          mapOf(
            "startTime" to LocalTime.of(10, 1),
            "endTime" to LocalTime.of(10, 0),
          ),
        ),
      )

      val result = service.validate(
        baselineCreate.copy(
          startTime = LocalTime.of(10, 1),
          endTime = LocalTime.of(10, 0),
        ),
        AppointmentValidationContext.Create(),
      )

      assertThat(result).hasErrors(expectedErrors)
      assertThat(result).hasNoWarnings()
    }
  }

  @Nested
  inner class PenaltyMinutesDuration {

    @Test
    fun `if no penalty minutes, do nothing`() {
      val result = service.validate(
        baselineCreate.copy(
          attendanceData = AttendanceDataDto.valid().copy(
            penaltyMinutes = null,
            penaltyTime = null,
          ),
        ),
        AppointmentValidationContext.Create(),
      )

      assertThat(result).hasNoErrors()
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `if penalty time is less than duration, do nothing`() {
      val result = service.validate(
        baselineCreate.copy(
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(16, 35),
          attendanceData = AttendanceDataDto.valid().copy(
            penaltyMinutes = null,
            penaltyTime = HourMinuteDuration(Duration.ofHours(6).plusMinutes(30)),
          ),
        ),
        AppointmentValidationContext.Create(),
      )

      assertThat(result).hasNoErrors()
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `if penalty minutes is less than duration, do nothing`() {
      val result = service.validate(
        baselineCreate.copy(
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(16, 35),
          attendanceData = AttendanceDataDto.valid().copy(
            penaltyMinutes = 390,
            penaltyTime = null,
          ),
        ),
        AppointmentValidationContext.Create(),
      )

      assertThat(result).hasNoErrors()
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `if penalty time is same as duration, do nothing`() {
      val result = service.validate(
        baselineCreate.copy(
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(16, 35),
          attendanceData = AttendanceDataDto.valid().copy(
            penaltyMinutes = null,
            penaltyTime = HourMinuteDuration(Duration.ofHours(6).plusMinutes(35)),
          ),
        ),
        AppointmentValidationContext.Create(),
      )

      assertThat(result).hasNoErrors()
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `if penalty minutes is same as duration, do nothing`() {
      val result = service.validate(
        baselineCreate.copy(
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(16, 35),
          attendanceData = AttendanceDataDto.valid().copy(
            penaltyMinutes = 395,
            penaltyTime = HourMinuteDuration(Duration.ofHours(6).plusMinutes(35)),
          ),
        ),
        AppointmentValidationContext.Create(),
      )

      assertThat(result).hasNoErrors()
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `if penalty time is greater than as duration, has error`() {
      val expectedErrors = listOf(
        ValidationResultItem(
          "$.penaltyMinutes",
          "PENALTY_DURATION_EXCEEDS_APPOINTMENT_DURATION",
          mapOf(
            "penaltyDuration" to Duration.ofHours(6).plusMinutes(36),
            "appointmentDuration" to Duration.ofHours(6).plusMinutes(35),
          ),
        ),
      )

      val result = service.validate(
        baselineCreate.copy(
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(16, 35),
          attendanceData = AttendanceDataDto.valid().copy(
            penaltyMinutes = null,
            penaltyTime = HourMinuteDuration(Duration.ofHours(6).plusMinutes(36)),
          ),
        ),
        AppointmentValidationContext.Create(),
      )

      assertThat(result).hasErrors(expectedErrors)
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `if penalty minutes is greater than as duration, has error`() {
      val expectedErrors = listOf(
        ValidationResultItem(
          "$.penaltyMinutes",
          "PENALTY_DURATION_EXCEEDS_APPOINTMENT_DURATION",
          mapOf(
            "penaltyDuration" to Duration.ofHours(6).plusMinutes(36),
            "appointmentDuration" to Duration.ofHours(6).plusMinutes(35),
          ),
        ),
      )

      val result = service.validate(
        baselineCreate.copy(
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(16, 35),
          attendanceData = AttendanceDataDto.valid().copy(
            penaltyMinutes = 396,
            penaltyTime = HourMinuteDuration(Duration.ofMinutes(5)),
          ),
        ),
        AppointmentValidationContext.Create(),
      )

      assertThat(result).hasErrors(expectedErrors)
      assertThat(result).hasNoWarnings()
    }
  }

  @Nested
  inner class Notes {

    @Test
    fun `null notes is accepted`() {
      val result = service.validate(
        baselineCreate.copy(notes = null),
        AppointmentValidationContext.Create(),
      )

      assertThat(result).hasNoErrors()
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `empty notes is accepted`() {
      val result = service.validate(
        baselineCreate.copy(notes = ""),
        AppointmentValidationContext.Create(),
      )

      assertThat(result).hasNoErrors()
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `notes length 4000 is accepted`() {
      val notes = "a".repeat(4000)
      val result = service.validate(
        baselineCreate.copy(notes = notes),
        AppointmentValidationContext.Create(),
      )

      assertThat(result).hasNoErrors()
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `notes length 4001 has error`() {
      val expectedErrors = listOf(
        ValidationResultItem(
          "$.notes",
          "NOTES_TOO_LONG",
          mapOf(
            "length" to 4001,
            "maxLength" to 4000,
          ),
        ),
      )

      val notes = "a".repeat(4001)
      val result = service.validate(
        baselineCreate.copy(notes = notes),
        AppointmentValidationContext.Create(),
      )

      assertThat(result).hasErrors(expectedErrors)
      assertThat(result).hasNoWarnings()
    }
  }

  @Nested
  inner class EteAllowanceRemaining {

    val eteProject = baselineProject.copy(
      projectType = ProjectTypeDto.valid().copy(
        group = ProjectTypeGroupDto.ETE,
      ),
    )

    val notEteProject = baselineProject.copy(
      projectType = ProjectTypeDto.valid().copy(
        group = ProjectTypeGroupDto.INDIVIDUAL,
      ),
    )

    @Test
    fun `Not ETE project, ignore remaining ETE allowance`() {
      every { projectService.getProject(PROJECT_CODE) } returns notEteProject
      every { appointmentCalculationService.minutesToCredit(any(), any(), any(), any()) } returns Duration.ofMinutes(120)

      val result = service.validate(baselineCreate, AppointmentValidationContext.Create())

      assertThat(result).hasNoErrors()
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `ETE project with sufficient remaining ETE allowance for appointment`() {
      every { appointmentCalculationService.minutesToCredit(any(), any(), any(), any()) } returns Duration.ofMinutes(60)
      every { projectService.getProject(PROJECT_CODE) } returns eteProject
      every { offenderService.getUnpaidWorkDetails(UPW_DETAILS_ID) } returns baselineUnpaidWorkDetails.copy(
        remainingEteMinutes = 60,
      )

      val result = service.validate(baselineCreate, AppointmentValidationContext.Create())

      assertThat(result).hasNoErrors()
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `ETE project with insufficient remaining ETE allowance for appointment, has error`() {
      every { appointmentCalculationService.minutesToCredit(any(), any(), any(), any()) } returns Duration.ofMinutes(61)
      every { projectService.getProject(PROJECT_CODE) } returns eteProject
      every { offenderService.getUnpaidWorkDetails(UPW_DETAILS_ID) } returns baselineUnpaidWorkDetails.copy(
        remainingEteMinutes = 60,
      )

      val expectedErrors = listOf(
        ValidationResultItem(
          "$..[\"startTime\", \"endTime\", \"penaltyMinutes\"]",
          "CREDITED_ETE_TIME_EXCEEDS_REMAINING_ETE_TIME",
          mapOf(
            "timeToCredit" to Duration.ofMinutes(61),
            "remainingEteTime" to Duration.ofMinutes(60),
          ),
        ),
      )

      val result = service.validate(baselineCreate, AppointmentValidationContext.Create())

      assertThat(result).hasErrors(expectedErrors)
      assertThat(result).hasNoWarnings()
    }
  }

  @Nested
  inner class PickUpLocation {
    @Test
    fun `has error when pickup location not found`() {
      every { providerService.getPickupLocation(TeamId(PROVIDER_CODE, TEAM_CODE), PICK_UP_LOCATION_CODE) } returns null

      val expectedErrors = listOf(
        ValidationResultItem(
          "$.pickUpLocationCode",
          "UNKNOWN_PICKUP_LOCATION",
          mapOf(
            "projectTeamCode" to UpdateAppointmentValidationServiceTest.TEAM_CODE,
            "locationCode" to UpdateAppointmentValidationServiceTest.PICK_UP_LOCATION_CODE,
          ),
        ),
      )

      val result = service.validate(baselineCreate, AppointmentValidationContext.Create())

      assertThat(result).hasErrors(expectedErrors)
      assertThat(result).hasNoWarnings()
    }
  }

  @Nested
  inner class MinutesToCredit {
    @Test
    fun `has error when time credited is more than total required time`() {
      every { appointmentCalculationService.minutesToCredit(any(), any(), any(), any()) } returns Duration.ofMinutes(61)
      every { offenderService.getUnpaidWorkDetails(UPW_DETAILS_ID) } returns baselineUnpaidWorkDetails.copy(
        requiredMinutes = 60,
        completedMinutes = 0,
        completedEteMinutes = 0,
        adjustments = 0,
      )

      val expectedErrors = listOf(
        ValidationResultItem(
          "$..[\"startTime\", \"endTime\", \"penaltyMinutes\"]",
          "CREDITED_TIME_EXCEEDS_REMAINING_REQUIREMENT_TIME",
          mapOf(
            "timeToCredit" to Duration.ofMinutes(61),
            "remainingRequirementTime" to Duration.ofMinutes(60),
          ),
        ),
      )

      val result = service.validate(baselineCreate, AppointmentValidationContext.Create())

      assertThat(result).hasErrors(expectedErrors)
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `has error when time credited is more than remaining required time`() {
      every { appointmentCalculationService.minutesToCredit(any(), any(), any(), any()) } returns Duration.ofMinutes(61)
      every { offenderService.getUnpaidWorkDetails(UPW_DETAILS_ID) } returns baselineUnpaidWorkDetails.copy(
        requiredMinutes = 120,
        completedMinutes = 60,
        completedEteMinutes = 0,
        adjustments = 0,
      )

      val expectedErrors = listOf(
        ValidationResultItem(
          "$..[\"startTime\", \"endTime\", \"penaltyMinutes\"]",
          "CREDITED_TIME_EXCEEDS_REMAINING_REQUIREMENT_TIME",
          mapOf(
            "timeToCredit" to Duration.ofMinutes(61),
            "remainingRequirementTime" to Duration.ofMinutes(60),
          ),
        ),
      )

      val result = service.validate(baselineCreate, AppointmentValidationContext.Create())

      assertThat(result).hasErrors(expectedErrors)
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `has error when time credited is more than remaining required time (including adjustments)`() {
      every { appointmentCalculationService.minutesToCredit(any(), any(), any(), any()) } returns Duration.ofMinutes(61)
      every { offenderService.getUnpaidWorkDetails(UPW_DETAILS_ID) } returns baselineUnpaidWorkDetails.copy(
        requiredMinutes = 180,
        completedMinutes = 60,
        completedEteMinutes = 0,
        adjustments = -60,
      )

      val expectedErrors = listOf(
        ValidationResultItem(
          "$..[\"startTime\", \"endTime\", \"penaltyMinutes\"]",
          "CREDITED_TIME_EXCEEDS_REMAINING_REQUIREMENT_TIME",
          mapOf(
            "timeToCredit" to Duration.ofMinutes(61),
            "remainingRequirementTime" to Duration.ofMinutes(60),
          ),
        ),
      )

      val result = service.validate(baselineCreate, AppointmentValidationContext.Create())

      assertThat(result).hasErrors(expectedErrors)
      assertThat(result).hasNoWarnings()
    }
  }
}
