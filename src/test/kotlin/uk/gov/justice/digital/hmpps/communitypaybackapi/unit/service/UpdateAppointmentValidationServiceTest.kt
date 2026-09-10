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
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AttendanceDataDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.OffenderDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.PickUpDataDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.PickUpLocationDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectAvailabilityDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectTypeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectTypeGroupDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SchedulingDayOfWeekDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UnpaidWorkDetailsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UnpaidWorkDetailsIdDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.validFull
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentCalculationService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentValidationService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentValidationService.AppointmentValidationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentValidationService.FindResult
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.OffenderService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.ProjectService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.ProviderService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.TeamId
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.UpdateAppointmentValidationService
import uk.gov.justice.digital.hmpps.communitypaybackapi.unit.common.validation.hasErrors
import uk.gov.justice.digital.hmpps.communitypaybackapi.unit.common.validation.hasNoErrors
import uk.gov.justice.digital.hmpps.communitypaybackapi.unit.common.validation.hasNoWarnings
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

@ExtendWith(MockKExtension::class)
class UpdateAppointmentValidationServiceTest {

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
  lateinit var service: UpdateAppointmentValidationService

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

  val baselineExistingAppointment = AppointmentDto.valid().copy(
    projectCode = PROJECT_CODE,
    deliusEventNumber = EVENT_NUMBER,
    offender = OffenderDto.validFull().copy(crn = CRN),
    contactOutcomeCode = null,
    pickUpData = PickUpDataDto.valid().copy(
      pickupLocation = PickUpLocationDto.valid().copy(
        deliusCode = PICK_UP_LOCATION_CODE,
      ),
    ),
    minutesCredited = 0,
  )
  val baselineUpdate = UpdateAppointmentDto.valid().copy(
    contactOutcomeCode = OUTCOME_CODE,
    date = LocalDate.of(2025, 1, 1),
    startTime = LocalTime.MIN,
    endTime = LocalTime.MAX,
  )
  val baselineOutcome = ContactOutcomeEntity.valid().copy(code = OUTCOME_CODE)
  val baselinePickUpLocation = PickUpLocationDto.valid()
  val baselineProject = ProjectDto.valid().copy(
    actualEndDateExclusive = null,
    projectType = ProjectTypeDto.valid().copy(group = ProjectTypeGroupDto.INDIVIDUAL),
    availability = SchedulingDayOfWeekDto.entries.map { dayOfWeek ->
      ProjectAvailabilityDto.valid().copy(dayOfWeek = dayOfWeek)
    },
    providerCode = PROVIDER_CODE,
    teamCode = TEAM_CODE,
  )
  val baselineUnpaidWorkDetails = UnpaidWorkDetailsDto.valid().copy(
    eventNumber = EVENT_NUMBER,
    sentenceDate = LocalDate.of(2025, 1, 1),
    requiredMinutes = 3600,
    completedMinutes = 1200,
    completedEteMinutes = 1200,
    adjustments = 0,
  )

  @BeforeEach
  fun setupBaselineMockResponses() {
    every { projectService.getProject(PROJECT_CODE) } returns baselineProject
    every { contactOutcomeEntityRepository.findByCode(OUTCOME_CODE) } returns baselineOutcome
    every { providerService.getPickupLocation(TeamId(PROVIDER_CODE, TEAM_CODE), PICK_UP_LOCATION_CODE) } returns baselinePickUpLocation
    every { offenderService.getUnpaidWorkDetails(UPW_DETAILS_ID) } returns baselineUnpaidWorkDetails
    every { appointmentCalculationService.minutesToCredit(any(), any(), any(), any()) } returns Duration.ofMinutes(60)
  }

  @Nested
  inner class Success {

    @Test
    fun `baseline request passes`() {
      val ctx = AppointmentValidationContext.Update(baselineExistingAppointment)
      val result = service.validate(baselineUpdate, ctx)

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
          startTime = baselineUpdate.startTime,
          endTime = baselineUpdate.endTime,
          penaltyMinutes = Duration.ofMinutes(55),
        )
      } returns Duration.ofMinutes(125)

      val update = baselineUpdate.copy(
        attendanceData = AttendanceDataDto.valid().copy(penaltyMinutes = 55L),
      )

      val ctx = AppointmentValidationService.AppointmentValidationContext.Update(baselineExistingAppointment)
      val result = service.validate(
        value = update,
        ctx = ctx,
      )

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
    fun `ok if updated date is before project end date`() {
      every { projectService.getProject(PROJECT_CODE) } returns baselineProject.copy(
        actualEndDateExclusive = LocalDate.of(2030, 5, 4),
      )
      val result = service.validate(
        value = baselineUpdate.copy(
          date = LocalDate.of(2030, 5, 3),
        ),
        ctx = AppointmentValidationService.AppointmentValidationContext.Update(baselineExistingAppointment),
      )

      assertThat(result).hasNoErrors()
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `has error if no update to date and existing date is on project end date`() {
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
        value = baselineUpdate.copy(
          date = LocalDate.of(2030, 5, 4),
        ),
        ctx = AppointmentValidationService.AppointmentValidationContext.Update(
          baselineExistingAppointment.copy(
            date = LocalDate.of(2030, 5, 4),
          ),
        ),
      )

      assertThat(result).hasErrors(expectedErrors)
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `has error if updated date is on project end date`() {
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
        value = baselineUpdate.copy(
          date = LocalDate.of(2030, 5, 4),
        ),
        ctx = AppointmentValidationService.AppointmentValidationContext.Update(baselineExistingAppointment),
      )

      assertThat(result).hasErrors(expectedErrors)
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `has error if no update to date and existing date is after project end date`() {
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
        value = baselineUpdate.copy(
          date = LocalDate.of(2030, 5, 5),
        ),
        ctx = AppointmentValidationService.AppointmentValidationContext.Update(
          baselineExistingAppointment.copy(
            date = LocalDate.of(2030, 5, 5),
          ),
        ),
      )

      assertThat(result).hasErrors(expectedErrors)
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `has error if updated date is after project end date`() {
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
        value = baselineUpdate.copy(
          date = LocalDate.of(2030, 5, 5),
        ),
        ctx = AppointmentValidationService.AppointmentValidationContext.Update(baselineExistingAppointment),
      )

      assertThat(result).hasErrors(expectedErrors)
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `has error if no update to date and existing date is before sentencing date`() {
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
        value = baselineUpdate.copy(
          date = LocalDate.of(2025, 1, 1),
        ),
        ctx = AppointmentValidationService.AppointmentValidationContext.Update(
          baselineExistingAppointment.copy(
            date = LocalDate.of(2025, 1, 1),
          ),
        ),
      )

      assertThat(result).hasErrors(expectedErrors)
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `has error if updated date is before sentencing date`() {
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
        value = baselineUpdate.copy(
          date = LocalDate.of(2025, 1, 1),
        ),
        ctx = AppointmentValidationService.AppointmentValidationContext.Update(baselineExistingAppointment),
      )

      assertThat(result).hasErrors(expectedErrors)
      assertThat(result).hasNoWarnings()
    }
  }

  @Nested
  inner class Availability {

    @Test
    fun `has error if update doesnt modify date and existing project isn't available on existing project's day of week`() {
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
        value = baselineUpdate.copy(
          date = LocalDate.of(2026, 2, 25),
        ),
        ctx = AppointmentValidationService.AppointmentValidationContext.Update(
          baselineExistingAppointment.copy(
            // Wednesday
            date = LocalDate.of(2026, 2, 25),
          ),
        ),
      )

      assertThat(result).hasErrors(expectedErrors)
      assertThat(result).hasNoWarnings()
    }

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
        value = baselineUpdate.copy(
          // Wednesday
          date = LocalDate.of(2026, 2, 25),
        ),
        ctx = AppointmentValidationService.AppointmentValidationContext.Update(baselineExistingAppointment),
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

      val result = service.validate(
        value = baselineUpdate,
        ctx = AppointmentValidationService.AppointmentValidationContext.Update(baselineExistingAppointment),
      )

      assertThat(result).hasErrors(expectedErrors)
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `if appointment ends in the past, attendance outcome is mandatory`() {
      val outcome = baselineOutcome.copy(attended = true, enforceable = false)
      every { contactOutcomeEntityRepository.findByCode(OUTCOME_CODE) } returns outcome

      val result = service.validate(
        value = baselineUpdate.copy(
          startTime = LocalTime.now().minusMinutes(1),
        ),
        ctx = AppointmentValidationService.AppointmentValidationContext.Update(baselineExistingAppointment.copy(date = LocalDate.now())),
      )

      assertThat(result).hasNoErrors()
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `if appointment is current or in the past, attendance outcome can be recorded`() {
      val outcome = baselineOutcome.copy(attended = true, enforceable = false)
      every { contactOutcomeEntityRepository.findByCode(OUTCOME_CODE) } returns outcome
      val result = service.validate(
        value = baselineUpdate.copy(
          startTime = LocalTime.now().minusMinutes(1),
        ),
        ctx = AppointmentValidationService.AppointmentValidationContext.Update(baselineExistingAppointment.copy(date = LocalDate.now())),
      )

      assertThat(result).hasNoErrors()
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `if appointment is current or in the past, enforceable outcome can be recorded`() {
      val outcome = baselineOutcome.copy(attended = false, enforceable = true)
      every { contactOutcomeEntityRepository.findByCode(OUTCOME_CODE) } returns outcome

      val result = service.validate(
        value = baselineUpdate.copy(
          startTime = LocalTime.now().minusMinutes(1),
        ),
        ctx = AppointmentValidationService.AppointmentValidationContext.Update(baselineExistingAppointment.copy(date = LocalDate.now())),
      )

      assertThat(result).hasNoErrors()
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `if appointment is current or in the past, non-enforceable absence outcome can be recorded`() {
      val outcome = baselineOutcome.copy(attended = false, enforceable = false)
      every { contactOutcomeEntityRepository.findByCode(OUTCOME_CODE) } returns outcome

      val result = service.validate(
        value = baselineUpdate.copy(
          startTime = LocalTime.now().minusMinutes(1),
        ),
        ctx = AppointmentValidationService.AppointmentValidationContext.Update(baselineExistingAppointment.copy(date = LocalDate.now())),
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
        value = baselineUpdate.copy(
          date = LocalDate.now().plusDays(1),
          startTime = LocalTime.NOON,
        ),
        ctx = AppointmentValidationService.AppointmentValidationContext.Update(baselineExistingAppointment),
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
        value = baselineUpdate.copy(
          date = LocalDate.now().plusDays(1),
          startTime = LocalTime.NOON,
        ),
        ctx = AppointmentValidationService.AppointmentValidationContext.Update(baselineExistingAppointment),
      )

      assertThat(result).hasErrors(expectedErrors)
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `if appointment is in future, non-enforceable absence can be recorded`() {
      val outcome = baselineOutcome.copy(attended = false, enforceable = false)
      every { contactOutcomeEntityRepository.findByCode(outcome.code) } returns outcome

      val result = service.validate(
        value = baselineUpdate.copy(
          date = LocalDate.now().plusDays(1),
          startTime = LocalTime.NOON,
        ),
        ctx = AppointmentValidationService.AppointmentValidationContext.Update(baselineExistingAppointment),
      )

      assertThat(result).hasNoErrors()
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `if outcome attended is false, attendance data isn't required`() {
      val outcome = baselineOutcome.copy(attended = false, enforceable = false)
      every { contactOutcomeEntityRepository.findByCode(outcome.code) } returns outcome

      val result = service.validate(
        value = baselineUpdate,
        ctx = AppointmentValidationService.AppointmentValidationContext.Update(baselineExistingAppointment.copy(date = LocalDate.now())),
      )

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
        value = baselineUpdate.copy(
          attendanceData = null,
        ),
        ctx = AppointmentValidationService.AppointmentValidationContext.Update(baselineExistingAppointment.copy(date = LocalDate.now())),
      )

      assertThat(result).hasErrors(expectedErrors)
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `if outcome attended is true and attendance data is provided, is success`() {
      val outcome = baselineOutcome.copy(attended = true, enforceable = false)
      every { contactOutcomeEntityRepository.findByCode(outcome.code) } returns outcome

      val result = service.validate(
        value = baselineUpdate.copy(
          attendanceData = AttendanceDataDto.valid(),
        ),
        ctx = AppointmentValidationService.AppointmentValidationContext.Update(baselineExistingAppointment.copy(date = LocalDate.now())),
      )

      assertThat(result).hasNoErrors()
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `existing contact outcome can be modified`() {
      every { contactOutcomeEntityRepository.findByCode("outcome1") } returns ContactOutcomeEntity.valid().copy(code = "outcome1", name = "outcome 1")
      every { contactOutcomeEntityRepository.findByCode("outcome2") } returns ContactOutcomeEntity.valid().copy(code = "outcome2")
      val existingAppointment = baselineExistingAppointment.copy(
        contactOutcomeCode = "outcome1",
        startTime = LocalTime.of(10, 0),
        endTime = LocalTime.of(11, 0),
      )

      val result = service.validate(
        value = baselineUpdate.copy(
          contactOutcomeCode = "outcome2",
          startTime = existingAppointment.startTime,
          endTime = existingAppointment.endTime,
        ),
        ctx = AppointmentValidationService.AppointmentValidationContext.Update(existingAppointment),
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
        value = baselineUpdate.copy(
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(10, 0),
        ),
        ctx = AppointmentValidationService.AppointmentValidationContext.Update(baselineExistingAppointment),
      )

      assertThat(result).hasErrors(expectedErrors)
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `if end time after start time, do nothing`() {
      val result = service.validate(
        value = baselineUpdate.copy(
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(10, 1),
        ),
        ctx = AppointmentValidationService.AppointmentValidationContext.Update(baselineExistingAppointment),
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
        value = baselineUpdate.copy(
          startTime = LocalTime.of(10, 1),
          endTime = LocalTime.of(10, 0),
        ),
        ctx = AppointmentValidationService.AppointmentValidationContext.Update(baselineExistingAppointment),
      )

      assertThat(result).hasErrors(expectedErrors)
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `if contact outcome is already set and start and end time remain the same, do nothing`() {
      val result = service.validate(
        value = baselineUpdate.copy(
          contactOutcomeCode = OUTCOME_CODE,
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(10, 1),
        ),
        ctx = AppointmentValidationService.AppointmentValidationContext.Update(
          baselineExistingAppointment.copy(
            contactOutcomeCode = OUTCOME_CODE,
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(10, 1),
          ),
        ),
      )

      assertThat(result).hasNoErrors()
      assertThat(result).hasNoWarnings()
    }
  }

  @Nested
  inner class PenaltyMinutesDuration {

    @BeforeEach
    fun setupOutcome() {
      every { contactOutcomeEntityRepository.findByCode(baselineOutcome.code) } returns baselineOutcome
    }

    @Test
    fun `if no penalty minutes, do nothing`() {
      val result = service.validate(
        value = baselineUpdate.copy(
          attendanceData = AttendanceDataDto.valid().copy(
            penaltyMinutes = null,
            penaltyTime = null,
          ),
        ),
        ctx = AppointmentValidationService.AppointmentValidationContext.Update(baselineExistingAppointment),
      )

      assertThat(result).hasNoErrors()
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `if penalty time is less than duration, do nothing`() {
      val result = service.validate(
        value = baselineUpdate.copy(
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(16, 35),
          attendanceData = AttendanceDataDto.valid().copy(
            penaltyMinutes = null,
            penaltyTime = HourMinuteDuration(Duration.ofHours(6).plusMinutes(30)),
          ),
        ),
        ctx = AppointmentValidationService.AppointmentValidationContext.Update(baselineExistingAppointment),
      )

      assertThat(result).hasNoErrors()
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `if penalty minutes is less than duration, do nothing`() {
      val result = service.validate(
        value = baselineUpdate.copy(
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(16, 35),
          attendanceData = AttendanceDataDto.valid().copy(
            penaltyMinutes = 390,
            penaltyTime = null,
          ),
        ),
        ctx = AppointmentValidationService.AppointmentValidationContext.Update(baselineExistingAppointment),
      )

      assertThat(result).hasNoErrors()
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `if penalty time is same as duration, do nothing`() {
      val result = service.validate(
        value = baselineUpdate.copy(
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(16, 35),
          attendanceData = AttendanceDataDto.valid().copy(
            penaltyMinutes = null,
            penaltyTime = HourMinuteDuration(Duration.ofHours(6).plusMinutes(35)),
          ),
        ),
        ctx = AppointmentValidationService.AppointmentValidationContext.Update(baselineExistingAppointment),
      )

      assertThat(result).hasNoErrors()
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `if penalty minutes is same as duration, do nothing`() {
      val result = service.validate(
        value = baselineUpdate.copy(
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(16, 35),
          attendanceData = AttendanceDataDto.valid().copy(
            penaltyMinutes = 395,
            penaltyTime = HourMinuteDuration(Duration.ofHours(6).plusMinutes(35)),
          ),
        ),
        ctx = AppointmentValidationService.AppointmentValidationContext.Update(baselineExistingAppointment),
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
        value = baselineUpdate.copy(
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(16, 35),
          attendanceData = AttendanceDataDto.valid().copy(
            penaltyMinutes = null,
            penaltyTime = HourMinuteDuration(Duration.ofHours(6).plusMinutes(36)),
          ),
        ),
        ctx = AppointmentValidationService.AppointmentValidationContext.Update(baselineExistingAppointment),
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
        value = baselineUpdate.copy(
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(16, 35),
          attendanceData = AttendanceDataDto.valid().copy(
            penaltyMinutes = 396,
            penaltyTime = HourMinuteDuration(Duration.ofMinutes(5)),
          ),
        ),
        ctx = AppointmentValidationService.AppointmentValidationContext.Update(baselineExistingAppointment),
      )

      assertThat(result).hasErrors(expectedErrors)
      assertThat(result).hasNoWarnings()
    }
  }

  @Nested
  inner class Notes {

    @BeforeEach
    fun setupOutcome() {
      every { contactOutcomeEntityRepository.findByCode(baselineOutcome.code) } returns baselineOutcome
    }

    @Test
    fun `null notes is accepted`() {
      val result = service.validate(
        value = baselineUpdate.copy(notes = null),
        ctx = AppointmentValidationService.AppointmentValidationContext.Update(baselineExistingAppointment),
      )

      assertThat(result).hasNoErrors()
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `empty notes is accepted`() {
      val result = service.validate(
        value = baselineUpdate.copy(notes = ""),
        ctx = AppointmentValidationService.AppointmentValidationContext.Update(baselineExistingAppointment),
      )

      assertThat(result).hasNoErrors()
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `notes length 4000 is accepted`() {
      val notes = "a".repeat(4000)
      val result = service.validate(
        value = baselineUpdate.copy(notes = notes),
        ctx = AppointmentValidationService.AppointmentValidationContext.Update(baselineExistingAppointment),
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
        value = baselineUpdate.copy(notes = notes),
        ctx = AppointmentValidationService.AppointmentValidationContext.Update(baselineExistingAppointment),
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

      val result = service.validate(
        value = baselineUpdate,
        ctx = AppointmentValidationService.AppointmentValidationContext.Update(baselineExistingAppointment),
      )

      assertThat(result).hasNoErrors()
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `ETE project with sufficient remaining ETE allowance for appointment`() {
      every { projectService.getProject(PROJECT_CODE) } returns eteProject
      every { appointmentCalculationService.minutesToCredit(any(), any(), any(), any()) } returns Duration.ofMinutes(60)
      every { offenderService.getUnpaidWorkDetails(UPW_DETAILS_ID) } returns baselineUnpaidWorkDetails.copy(
        remainingEteMinutes = 60,
      )

      val result = service.validate(
        value = baselineUpdate,
        ctx = AppointmentValidationService.AppointmentValidationContext.Update(
          baselineExistingAppointment.copy(
            minutesCredited = null,
          ),
        ),
      )

      assertThat(result).hasNoErrors()
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `ETE project with insufficient remaining ETE allowance for appointment, has error`() {
      every { projectService.getProject(PROJECT_CODE) } returns eteProject
      every { appointmentCalculationService.minutesToCredit(any(), any(), any(), any()) } returns Duration.ofMinutes(61)
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

      val result = service.validate(
        value = baselineUpdate,
        ctx = AppointmentValidationService.AppointmentValidationContext.Update(
          baselineExistingAppointment.copy(
            minutesCredited = null,
          ),
        ),
      )

      assertThat(result).hasErrors(expectedErrors)
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `Ensure existing minutes credited aren't 'double counted' when updating minutes credited`() {
      every { projectService.getProject(PROJECT_CODE) } returns eteProject
      every { appointmentCalculationService.minutesToCredit(any(), any(), any(), any()) } returns Duration.ofMinutes(120)
      every { offenderService.getUnpaidWorkDetails(UPW_DETAILS_ID) } returns baselineUnpaidWorkDetails.copy(
        remainingEteMinutes = 20,
      )

      val result = service.validate(
        value = baselineUpdate,
        ctx = AppointmentValidationService.AppointmentValidationContext.Update(
          baselineExistingAppointment.copy(
            minutesCredited = 100,
          ),
        ),
      )

      assertThat(result).hasNoErrors()
      assertThat(result).hasNoWarnings()
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
              "projectTeamCode" to TEAM_CODE,
              "locationCode" to PICK_UP_LOCATION_CODE,
            ),
          ),
        )

        val result = service.validate(
          value = baselineUpdate,
          ctx = AppointmentValidationService.AppointmentValidationContext.Update(baselineExistingAppointment),
        )

        assertThat(result).hasErrors(expectedErrors)
        assertThat(result).hasNoWarnings()
      }
    }
  }

  @Nested
  inner class Sensitive {
    @Test
    fun `has error when existing appointment is sensitive but the update is not`() {
      val existingAppointment = baselineExistingAppointment.copy(sensitive = true)
      val update = baselineUpdate.copy(sensitive = false)

      val expectedErrors = listOf(
        ValidationResultItem(
          "$.sensitive",
          "APPOINTMENT_IS_SENSITIVE",
          mapOf(),
        ),
      )

      val result = service.validate(
        value = update,
        ctx = AppointmentValidationService.AppointmentValidationContext.Update(existingAppointment),
      )

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

      val result = service.validate(
        value = baselineUpdate,
        ctx = AppointmentValidationService.AppointmentValidationContext.Update(baselineExistingAppointment),
      )

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

      val result = service.validate(
        value = baselineUpdate,
        ctx = AppointmentValidationService.AppointmentValidationContext.Update(baselineExistingAppointment),
      )

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

      val result = service.validate(
        value = baselineUpdate,
        ctx = AppointmentValidationService.AppointmentValidationContext.Update(baselineExistingAppointment),
      )

      assertThat(result).hasErrors(expectedErrors)
      assertThat(result).hasNoWarnings()
    }

    @Test
    fun `Ensure existing minutes credited aren't 'double counted' when updating minutes credited`() {
      every { appointmentCalculationService.minutesToCredit(any(), any(), any(), any()) } returns Duration.ofMinutes(100)
      every { offenderService.getUnpaidWorkDetails(UPW_DETAILS_ID) } returns baselineUnpaidWorkDetails.copy(
        requiredMinutes = 120,
        completedMinutes = 20,
        completedEteMinutes = 20,
        adjustments = 0,
      )

      val result = service.validate(
        value = baselineUpdate,
        ctx = AppointmentValidationService.AppointmentValidationContext.Update(baselineExistingAppointment.copy(minutesCredited = 20)),
      )

      assertThat(result).hasNoErrors()
      assertThat(result).hasNoWarnings()
    }
  }
}
