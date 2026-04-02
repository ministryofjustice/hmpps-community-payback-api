package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.HourMinuteDuration
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AttendanceDataDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.OffenderDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectAvailabilityDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectTypeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectTypeGroupDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SchedulingDayOfWeekDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UnpaidWorkDetailsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.BadRequestException
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.validFull
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentCalculationService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentValidationService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentValidationService.ValidatedAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.OffenderService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.ProjectService
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

@ExtendWith(MockKExtension::class)
class AppointmentValidationServiceTest {

  @MockK
  lateinit var contactOutcomeEntityRepository: ContactOutcomeEntityRepository

  @MockK
  lateinit var offenderService: OffenderService

  @MockK
  lateinit var projectService: ProjectService

  @MockK
  lateinit var appointmentCalculationService: AppointmentCalculationService

  @InjectMockKs
  lateinit var service: AppointmentValidationService

  companion object {
    const val CRN = "CRN123"
    const val EVENT_NUMBER = 1234321
    const val OUTCOME_CODE = "OUTCOME1"
    const val PROJECT_CODE = "PROJ123"
  }

  @Nested
  inner class Create {

    val baselineCreate = CreateAppointmentDto.valid().copy(
      crn = CRN,
      date = LocalDate.of(2025, 1, 1),
      deliusEventNumber = EVENT_NUMBER,
      projectCode = PROJECT_CODE,
      contactOutcomeCode = OUTCOME_CODE,
      startTime = LocalTime.MIN,
      endTime = LocalTime.MAX,
    )
    val baselineOutcome = ContactOutcomeEntity.valid().copy(code = OUTCOME_CODE)
    val baselineProject = ProjectDto.valid().copy(
      actualEndDateExclusive = null,
      availability = SchedulingDayOfWeekDto.entries.map { dayOfWeek ->
        ProjectAvailabilityDto.valid().copy(dayOfWeek = dayOfWeek)
      },
      projectType = ProjectTypeDto.valid().copy(group = ProjectTypeGroupDto.INDIVIDUAL),
    )
    val baselineUnpaidWorkDetails = UnpaidWorkDetailsDto.valid().copy(
      eventNumber = EVENT_NUMBER,
      sentenceDate = baselineCreate.date,
    )

    @BeforeEach
    fun setupBaselineMockResponses() {
      every { projectService.getProject(PROJECT_CODE) } returns baselineProject
      every { contactOutcomeEntityRepository.findByCode(baselineOutcome.code) } returns baselineOutcome
      every { offenderService.getUnpaidWorkDetails(CRN, EVENT_NUMBER) } returns baselineUnpaidWorkDetails
      every { appointmentCalculationService.minutesToCredit(any(), any(), any(), any()) } returns Duration.ofMinutes(60)
    }

    @Nested
    inner class Success {

      @Test
      fun `baseline request passes`() {
        val result = service.validateCreate(baselineCreate)

        assertThat(result).isEqualTo(
          ValidatedAppointment(
            dto = baselineCreate,
            minutesToCredit = Duration.ofMinutes(60),
            contactOutcome = baselineOutcome,
            project = baselineProject,
          ),
        )
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
        val result = service.validateCreate(create)

        assertThat(result).isEqualTo(
          ValidatedAppointment(
            dto = create,
            minutesToCredit = Duration.ofMinutes(125),
            contactOutcome = baselineOutcome,
            project = baselineProject,
          ),
        )
      }
    }

    @Nested
    inner class Date {

      @Test
      fun `ok if date is before project end date`() {
        every { projectService.getProject(PROJECT_CODE) } returns baselineProject.copy(
          actualEndDateExclusive = LocalDate.of(2030, 5, 4),
        )

        service.validateCreate(
          baselineCreate.copy(
            date = LocalDate.of(2030, 5, 3),
          ),
        )
      }

      @Test
      fun `throws BadRequestException if date is on project end date`() {
        every { projectService.getProject(PROJECT_CODE) } returns baselineProject.copy(
          actualEndDateExclusive = LocalDate.of(2030, 5, 4),
        )

        assertThatThrownBy {
          service.validateCreate(
            baselineCreate.copy(
              date = LocalDate.of(2030, 5, 4),
            ),
          )
        }.isInstanceOf(BadRequestException::class.java)
          .hasMessage("Appointment Date of 04/05/2030 must be before project end date 04/05/2030")
      }

      @Test
      fun `throws BadRequestException if date is after project end date`() {
        every { projectService.getProject(PROJECT_CODE) } returns baselineProject.copy(
          actualEndDateExclusive = LocalDate.of(2030, 5, 4),
        )

        assertThatThrownBy {
          service.validateCreate(
            baselineCreate.copy(
              date = LocalDate.of(2030, 5, 5),
            ),
          )
        }.isInstanceOf(BadRequestException::class.java)
          .hasMessage("Appointment Date of 05/05/2030 must be before project end date 04/05/2030")
      }

      @Test
      fun `throws BadRequestException if date is before sentencing date`() {
        every { offenderService.getUnpaidWorkDetails(CRN, EVENT_NUMBER) } returns UnpaidWorkDetailsDto.valid().copy(
          eventNumber = EVENT_NUMBER,
          sentenceDate = LocalDate.of(2025, 1, 2),
        )

        assertThatThrownBy {
          service.validateCreate(
            baselineCreate.copy(
              date = LocalDate.of(2025, 1, 1),
            ),
          )
        }.isInstanceOf(BadRequestException::class.java)
          .hasMessage("Appointment Date of 01/01/2025 must be on or after sentence date of 02/01/2025")
      }
    }

    @Nested
    inner class Availability {

      @Test
      fun `throws BadRequestException if project isn't available on requested day of week`() {
        every { projectService.getProject(PROJECT_CODE) } returns baselineProject.copy(
          availability = SchedulingDayOfWeekDto.entries.filter { it != SchedulingDayOfWeekDto.WEDNESDAY }.map { dayOfWeek ->
            ProjectAvailabilityDto.valid().copy(dayOfWeek = dayOfWeek)
          },
        )

        assertThatThrownBy {
          service.validateCreate(
            baselineCreate.copy(
              // Wednesday
              date = LocalDate.of(2026, 2, 25),
            ),
          )
        }.isInstanceOf(BadRequestException::class.java)
          .hasMessage("Project is not available on Wednesday. Available days are Monday, Tuesday, Thursday, Friday, Saturday, Sunday")
      }
    }

    @Nested
    inner class ContactOutcome {

      @Test
      fun `throws BadRequestException when contact outcome not found`() {
        every { contactOutcomeEntityRepository.findByCode(OUTCOME_CODE) } returns null

        assertThatThrownBy {
          service.validateCreate(baselineCreate)
        }.isInstanceOf(BadRequestException::class.java)
          .hasMessage("Contact outcome not found for code '$OUTCOME_CODE'")
      }

      @Test
      fun `if appointment ended in the past, an outcome is mandatory`() {
        val outcome = baselineOutcome.copy(attended = true, enforceable = false)
        every { contactOutcomeEntityRepository.findByCode(OUTCOME_CODE) } returns outcome

        assertThatThrownBy {
          service.validateCreate(
            baselineCreate.copy(
              date = LocalDate.now(),
              startTime = LocalTime.now().minusMinutes(2),
              endTime = LocalTime.now().minusMinutes(1),
              contactOutcomeCode = null,
            ),
          )
        }.isInstanceOf(BadRequestException::class.java)
          .hasMessage("As the appointment is now complete a contact outcome is required")
      }

      @Test
      fun `if appointment is current or in the past, attendance outcome can be recorded`() {
        val outcome = baselineOutcome.copy(attended = true, enforceable = false)
        every { contactOutcomeEntityRepository.findByCode(OUTCOME_CODE) } returns outcome

        service.validateCreate(
          baselineCreate.copy(
            date = LocalDate.now(),
            startTime = LocalTime.now().minusMinutes(1),
          ),
        )
      }

      @Test
      fun `if appointment is current or in the past, enforceable outcome can be recorded`() {
        val outcome = baselineOutcome.copy(attended = false, enforceable = true)
        every { contactOutcomeEntityRepository.findByCode(OUTCOME_CODE) } returns outcome

        service.validateCreate(
          baselineCreate.copy(
            date = LocalDate.now(),
            startTime = LocalTime.now().minusMinutes(1),
          ),
        )
      }

      @Test
      fun `if appointment is current or in the past, non enforceable outcome can be recorded`() {
        val outcome = baselineOutcome.copy(attended = false, enforceable = false)
        every { contactOutcomeEntityRepository.findByCode(OUTCOME_CODE) } returns outcome

        service.validateCreate(
          baselineCreate.copy(
            date = LocalDate.now(),
            startTime = LocalTime.now().minusMinutes(1),
          ),
        )
      }

      @Test
      fun `if appointment is in future, attendance outcome can't be recorded`() {
        val outcome = baselineOutcome.copy(attended = true, enforceable = false)
        every { contactOutcomeEntityRepository.findByCode(OUTCOME_CODE) } returns outcome

        assertThatThrownBy {
          service.validateCreate(
            baselineCreate.copy(
              date = LocalDate.now(),
              startTime = LocalTime.now().plusMinutes(1),
            ),
          )
        }.isInstanceOf(BadRequestException::class.java)
          .hasMessage("As the appointment is in the future only acceptable absence outcomes can be recorded")
      }

      @Test
      fun `if appointment is in future, enforceable outcome can't be recorded`() {
        val outcome = baselineOutcome.copy(attended = false, enforceable = true)
        every { contactOutcomeEntityRepository.findByCode(outcome.code) } returns outcome

        assertThatThrownBy {
          service.validateCreate(
            baselineCreate.copy(
              date = LocalDate.now(),
              startTime = LocalTime.now().plusMinutes(1),
            ),
          )
        }.isInstanceOf(BadRequestException::class.java)
          .hasMessage("As the appointment is in the future only acceptable absence outcomes can be recorded")
      }

      @Test
      fun `if appointment is in future, non-enforceable absence can be recorded`() {
        val outcome = baselineOutcome.copy(attended = false, enforceable = false)
        every { contactOutcomeEntityRepository.findByCode(outcome.code) } returns outcome

        service.validateCreate(
          baselineCreate.copy(
            date = LocalDate.now(),
            startTime = LocalTime.now().plusMinutes(1),
          ),
        )
      }

      @Test
      fun `if outcome attended is false, attendance data isn't required`() {
        val outcome = baselineOutcome.copy(attended = false, enforceable = false)
        every { contactOutcomeEntityRepository.findByCode(outcome.code) } returns outcome

        service.validateCreate(baselineCreate.copy(date = LocalDate.now()))
      }

      @Test
      fun `if outcome attended is true and attendance data isn't provided, throw exception`() {
        val outcome = baselineOutcome.copy(attended = true, enforceable = false)
        every { contactOutcomeEntityRepository.findByCode(outcome.code) } returns outcome

        assertThatThrownBy {
          service.validateCreate(
            baselineCreate.copy(
              date = LocalDate.now(),
              attendanceData = null,
            ),
          )
        }.isInstanceOf(BadRequestException::class.java)
          .hasMessage("Attendance data is required for contact outcomes that indicate attendance")
      }

      @Test
      fun `if outcome attended is true and attendance data is provided, don't throw exception`() {
        val outcome = baselineOutcome.copy(attended = true, enforceable = false)
        every { contactOutcomeEntityRepository.findByCode(outcome.code) } returns outcome

        service.validateCreate(
          baselineCreate.copy(
            date = LocalDate.now(),
            attendanceData = AttendanceDataDto.valid(),
          ),
        )
      }
    }

    @Nested
    inner class AppointmentDuration {

      @BeforeEach
      fun setupOutcome() {
        every { contactOutcomeEntityRepository.findByCode(baselineOutcome.code) } returns baselineOutcome
      }

      @Test
      fun `if end time same as start time, throw exception`() {
        assertThatThrownBy {
          service.validateCreate(
            baselineCreate.copy(
              startTime = LocalTime.of(10, 0),
              endTime = LocalTime.of(10, 0),
            ),
          )
        }.isInstanceOf(BadRequestException::class.java)
          .hasMessage("End Time '10:00' must be after Start Time '10:00'")
      }

      @Test
      fun `if end time after start time, do nothing`() {
        service.validateCreate(
          baselineCreate.copy(
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(10, 1),
          ),
        )
      }

      @Test
      fun `if end time before start time, throw exception`() {
        assertThatThrownBy {
          service.validateCreate(
            baselineCreate.copy(
              startTime = LocalTime.of(10, 1),
              endTime = LocalTime.of(10, 0),
            ),
          )
        }.isInstanceOf(BadRequestException::class.java)
          .hasMessage("End Time '10:00' must be after Start Time '10:01'")
      }
    }

    @Nested
    inner class PenaltyMinutesDuration {

      @Test
      fun `if no penalty minutes, do nothing`() {
        service.validateCreate(
          baselineCreate.copy(
            attendanceData = AttendanceDataDto.valid().copy(
              penaltyMinutes = null,
              penaltyTime = null,
            ),
          ),
        )
      }

      @Test
      fun `if penalty time is less than duration, do nothing`() {
        service.validateCreate(
          baselineCreate.copy(
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(16, 35),
            attendanceData = AttendanceDataDto.valid().copy(
              penaltyMinutes = null,
              penaltyTime = HourMinuteDuration(Duration.ofHours(6).plusMinutes(30)),
            ),
          ),
        )
      }

      @Test
      fun `if penalty minutes is less than duration, do nothing`() {
        service.validateCreate(
          baselineCreate.copy(
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(16, 35),
            attendanceData = AttendanceDataDto.valid().copy(
              penaltyMinutes = 390,
              penaltyTime = null,
            ),
          ),
        )
      }

      @Test
      fun `if penalty time is same as duration, do nothing`() {
        service.validateCreate(
          baselineCreate.copy(
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(16, 35),
            attendanceData = AttendanceDataDto.valid().copy(
              penaltyMinutes = null,
              penaltyTime = HourMinuteDuration(Duration.ofHours(6).plusMinutes(35)),
            ),
          ),
        )
      }

      @Test
      fun `if penalty minutes is same as duration, do nothing`() {
        service.validateCreate(
          baselineCreate.copy(
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(16, 35),
            attendanceData = AttendanceDataDto.valid().copy(
              penaltyMinutes = 395,
              penaltyTime = HourMinuteDuration(Duration.ofHours(6).plusMinutes(35)),
            ),
          ),
        )
      }

      @Test
      fun `if penalty time is greater than as duration, throw exception`() {
        assertThatThrownBy {
          service.validateCreate(
            baselineCreate.copy(
              startTime = LocalTime.of(10, 0),
              endTime = LocalTime.of(16, 35),
              attendanceData = AttendanceDataDto.valid().copy(
                penaltyMinutes = null,
                penaltyTime = HourMinuteDuration(Duration.ofHours(6).plusMinutes(36)),
              ),
            ),
          )
        }.isInstanceOf(BadRequestException::class.java)
          .hasMessage("Penalty duration '6 hours 36 minutes' is greater than appointment duration '6 hours 35 minutes'")
      }

      @Test
      fun `if penalty minutes is greater than as duration, throw exception`() {
        assertThatThrownBy {
          service.validateCreate(
            baselineCreate.copy(
              startTime = LocalTime.of(10, 0),
              endTime = LocalTime.of(16, 35),
              attendanceData = AttendanceDataDto.valid().copy(
                penaltyMinutes = 396,
                penaltyTime = HourMinuteDuration(Duration.ofMinutes(5)),
              ),
            ),
          )
        }.isInstanceOf(BadRequestException::class.java)
          .hasMessage("Penalty duration '6 hours 36 minutes' is greater than appointment duration '6 hours 35 minutes'")
      }
    }

    @Nested
    inner class NotesValidation {

      @Test
      fun `null notes is accepted`() {
        assertThatCode {
          service.validateCreate(
            baselineCreate.copy(notes = null),
          )
        }.doesNotThrowAnyException()
      }

      @Test
      fun `empty notes is accepted`() {
        assertThatCode {
          service.validateCreate(
            baselineCreate.copy(notes = ""),
          )
        }.doesNotThrowAnyException()
      }

      @Test
      fun `notes length 4000 is accepted`() {
        val notes = "a".repeat(4000)
        assertThatCode {
          service.validateCreate(
            baselineCreate.copy(notes = notes),
          )
        }.doesNotThrowAnyException()
      }

      @Test
      fun `notes length 4001 throws BadRequest`() {
        val notes = "a".repeat(4001)
        assertThatThrownBy {
          service.validateCreate(
            baselineCreate.copy(notes = notes),
          )
        }.isInstanceOf(BadRequestException::class.java)
          .hasMessage("Notes must be fewer than 4000 characters")
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

        service.validateCreate(baselineCreate)
      }

      @Test
      fun `ETE project with sufficient remaining ETE allowance for appointment`() {
        every { appointmentCalculationService.minutesToCredit(any(), any(), any(), any()) } returns Duration.ofMinutes(60)
        every { projectService.getProject(PROJECT_CODE) } returns eteProject
        every { offenderService.getUnpaidWorkDetails(CRN, EVENT_NUMBER) } returns baselineUnpaidWorkDetails.copy(
          remainingEteMinutes = 60,
        )

        service.validateCreate(baselineCreate)
      }

      @Test
      fun `ETE project with insufficient remaining ETE allowance for appointment, throw exception`() {
        every { appointmentCalculationService.minutesToCredit(any(), any(), any(), any()) } returns Duration.ofMinutes(61)
        every { projectService.getProject(PROJECT_CODE) } returns eteProject
        every { offenderService.getUnpaidWorkDetails(CRN, EVENT_NUMBER) } returns baselineUnpaidWorkDetails.copy(
          remainingEteMinutes = 60,
        )

        assertThatThrownBy {
          service.validateCreate(baselineCreate)
        }.isInstanceOf(BadRequestException::class.java)
          .hasMessage("Credited minutes of '1 hours 1 minutes' exceeds remaining allowed ETE time of '1 hours 0 minutes'")
      }
    }
  }

  @Nested
  inner class Update {

    val baselineExistingAppointment = AppointmentDto.valid().copy(
      projectCode = PROJECT_CODE,
      deliusEventNumber = EVENT_NUMBER,
      offender = OffenderDto.validFull().copy(crn = CRN),
      contactOutcomeCode = null,
    )
    val baselineUpdate = UpdateAppointmentOutcomeDto.valid().copy(
      contactOutcomeCode = OUTCOME_CODE,
      startTime = LocalTime.MIN,
      endTime = LocalTime.MAX,
    )
    val baselineOutcome = ContactOutcomeEntity.valid().copy(code = OUTCOME_CODE)
    val baselineProject = ProjectDto.valid().copy(projectType = ProjectTypeDto.valid().copy(group = ProjectTypeGroupDto.INDIVIDUAL))
    val baselineUnpaidWorkDetails = UnpaidWorkDetailsDto.valid().copy(
      eventNumber = EVENT_NUMBER,
    )

    @BeforeEach
    fun setupBaselineMockResponses() {
      every { projectService.getProject(PROJECT_CODE) } returns baselineProject
      every { contactOutcomeEntityRepository.findByCode(OUTCOME_CODE) } returns baselineOutcome
      every { offenderService.getUnpaidWorkDetails(CRN, EVENT_NUMBER) } returns baselineUnpaidWorkDetails
      every { appointmentCalculationService.minutesToCredit(any(), any(), any(), any()) } returns Duration.ofMinutes(60)
    }

    @Nested
    inner class Success {

      @Test
      fun `baseline request passes`() {
        val result = service.validateUpdate(
          appointment = baselineExistingAppointment,
          update = baselineUpdate,
        )

        assertThat(result).isEqualTo(
          ValidatedAppointment(
            dto = baselineUpdate,
            minutesToCredit = Duration.ofMinutes(60),
            contactOutcome = baselineOutcome,
            project = baselineProject,
          ),
        )
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

        val result = service.validateUpdate(
          appointment = baselineExistingAppointment,
          update = update,
        )

        assertThat(result).isEqualTo(
          ValidatedAppointment(
            dto = update,
            minutesToCredit = Duration.ofMinutes(125),
            contactOutcome = baselineOutcome,
            project = baselineProject,
          ),
        )
      }
    }

    @Nested
    inner class ContactOutcome {

      @Test
      fun `throws BadRequestException when contact outcome not found`() {
        every { contactOutcomeEntityRepository.findByCode(OUTCOME_CODE) } returns null

        assertThatThrownBy {
          service.validateUpdate(
            appointment = baselineExistingAppointment,
            update = baselineUpdate,
          )
        }.isInstanceOf(BadRequestException::class.java)
          .hasMessage("Contact outcome not found for code '$OUTCOME_CODE'")
      }

      @Test
      fun `if appointment ends in the past, attendance outcome is mandatory`() {
        val outcome = baselineOutcome.copy(attended = true, enforceable = false)
        every { contactOutcomeEntityRepository.findByCode(OUTCOME_CODE) } returns outcome

        service.validateUpdate(
          appointment = baselineExistingAppointment.copy(date = LocalDate.now()),
          update = baselineUpdate.copy(
            startTime = LocalTime.now().minusMinutes(1),
          ),
        )
      }

      @Test
      fun `if appointment is current or in the past, attendance outcome can be recorded`() {
        val outcome = baselineOutcome.copy(attended = true, enforceable = false)
        every { contactOutcomeEntityRepository.findByCode(OUTCOME_CODE) } returns outcome

        service.validateUpdate(
          appointment = baselineExistingAppointment.copy(date = LocalDate.now()),
          update = baselineUpdate.copy(
            startTime = LocalTime.now().minusMinutes(1),
          ),
        )
      }

      @Test
      fun `if appointment is current or in the past, enforceable outcome can be recorded`() {
        val outcome = baselineOutcome.copy(attended = false, enforceable = true)
        every { contactOutcomeEntityRepository.findByCode(OUTCOME_CODE) } returns outcome

        service.validateUpdate(
          appointment = baselineExistingAppointment.copy(date = LocalDate.now()),
          update = baselineUpdate.copy(
            startTime = LocalTime.now().minusMinutes(1),
          ),
        )
      }

      @Test
      fun `if appointment is current or in the past, non-enforceable absence outcome can be recorded`() {
        val outcome = baselineOutcome.copy(attended = false, enforceable = false)
        every { contactOutcomeEntityRepository.findByCode(OUTCOME_CODE) } returns outcome

        service.validateUpdate(
          appointment = baselineExistingAppointment.copy(date = LocalDate.now()),
          update = baselineUpdate.copy(
            startTime = LocalTime.now().minusMinutes(1),
          ),
        )
      }

      @Test
      fun `if appointment is in future, attendance outcome can't be recorded`() {
        val outcome = baselineOutcome.copy(attended = true, enforceable = false)
        every { contactOutcomeEntityRepository.findByCode(OUTCOME_CODE) } returns outcome

        assertThatThrownBy {
          service.validateUpdate(
            appointment = baselineExistingAppointment.copy(date = LocalDate.now()),
            update = baselineUpdate.copy(
              startTime = LocalTime.now().plusMinutes(1),
            ),
          )
        }.isInstanceOf(BadRequestException::class.java)
          .hasMessage("As the appointment is in the future only acceptable absence outcomes can be recorded")
      }

      @Test
      fun `if appointment is in future, enforceable outcome can't be recorded`() {
        val outcome = baselineOutcome.copy(attended = false, enforceable = true)
        every { contactOutcomeEntityRepository.findByCode(outcome.code) } returns outcome

        assertThatThrownBy {
          service.validateUpdate(
            appointment = baselineExistingAppointment.copy(date = LocalDate.now()),
            update = baselineUpdate.copy(
              startTime = LocalTime.now().plusMinutes(1),
            ),
          )
        }.isInstanceOf(BadRequestException::class.java)
          .hasMessage("As the appointment is in the future only acceptable absence outcomes can be recorded")
      }

      @Test
      fun `if appointment is in future, non-enforceable absence can be recorded`() {
        val outcome = baselineOutcome.copy(attended = false, enforceable = false)
        every { contactOutcomeEntityRepository.findByCode(outcome.code) } returns outcome

        service.validateUpdate(
          appointment = baselineExistingAppointment.copy(date = LocalDate.now()),
          update = baselineUpdate.copy(
            startTime = LocalTime.now().plusMinutes(1),
          ),
        )
      }

      @Test
      fun `if outcome attended is false, attendance data isn't required`() {
        val outcome = baselineOutcome.copy(attended = false, enforceable = false)
        every { contactOutcomeEntityRepository.findByCode(outcome.code) } returns outcome

        service.validateUpdate(
          appointment = baselineExistingAppointment.copy(date = LocalDate.now()),
          update = baselineUpdate,
        )
      }

      @Test
      fun `if outcome attended is true and attendance data isn't provided, throw exception`() {
        val outcome = baselineOutcome.copy(attended = true, enforceable = false)
        every { contactOutcomeEntityRepository.findByCode(outcome.code) } returns outcome

        assertThatThrownBy {
          service.validateUpdate(
            appointment = baselineExistingAppointment.copy(date = LocalDate.now()),
            update = baselineUpdate.copy(
              attendanceData = null,
            ),
          )
        }.isInstanceOf(BadRequestException::class.java)
          .hasMessage("Attendance data is required for contact outcomes that indicate attendance")
      }

      @Test
      fun `if outcome attended is true and attendance data is provided, don't throw exception`() {
        val outcome = baselineOutcome.copy(attended = true, enforceable = false)
        every { contactOutcomeEntityRepository.findByCode(outcome.code) } returns outcome

        service.validateUpdate(
          appointment = baselineExistingAppointment.copy(date = LocalDate.now()),
          update = baselineUpdate.copy(
            attendanceData = AttendanceDataDto.valid(),
          ),
        )
      }

      @Test
      fun `existing contact outcome cannot be modified`() {
        every { contactOutcomeEntityRepository.findByCode("outcome1") } returns ContactOutcomeEntity.valid().copy(code = "outcome1", name = "outcome 1")
        every { contactOutcomeEntityRepository.findByCode("outcome2") } returns ContactOutcomeEntity.valid().copy(code = "outcome2")

        assertThatThrownBy {
          service.validateUpdate(
            appointment = baselineExistingAppointment.copy(
              contactOutcomeCode = "outcome1",
            ),
            update = baselineUpdate.copy(
              contactOutcomeCode = "outcome2",
            ),
          )
        }.isInstanceOf(BadRequestException::class.java)
          .hasMessage("The existing contact outcome of 'outcome 1' cannot be modified")
      }
    }

    @Nested
    inner class AppointmentDuration {

      @BeforeEach
      fun setupOutcome() {
        every { contactOutcomeEntityRepository.findByCode(baselineOutcome.code) } returns baselineOutcome
      }

      @Test
      fun `if end time same as start time, throw exception`() {
        assertThatThrownBy {
          service.validateUpdate(
            appointment = baselineExistingAppointment,
            baselineUpdate.copy(
              startTime = LocalTime.of(10, 0),
              endTime = LocalTime.of(10, 0),
            ),
          )
        }.isInstanceOf(BadRequestException::class.java)
          .hasMessage("End Time '10:00' must be after Start Time '10:00'")
      }

      @Test
      fun `if end time after start time, do nothing`() {
        service.validateUpdate(
          appointment = baselineExistingAppointment,
          baselineUpdate.copy(
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(10, 1),
          ),
        )
      }

      @Test
      fun `if end time before start time, throw exception`() {
        assertThatThrownBy {
          service.validateUpdate(
            appointment = baselineExistingAppointment,
            baselineUpdate.copy(
              startTime = LocalTime.of(10, 1),
              endTime = LocalTime.of(10, 0),
            ),
          )
        }.isInstanceOf(BadRequestException::class.java)
          .hasMessage("End Time '10:00' must be after Start Time '10:01'")
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
        service.validateUpdate(
          appointment = baselineExistingAppointment,
          baselineUpdate.copy(
            attendanceData = AttendanceDataDto.valid().copy(
              penaltyMinutes = null,
              penaltyTime = null,
            ),
          ),
        )
      }

      @Test
      fun `if penalty time is less than duration, do nothing`() {
        service.validateUpdate(
          appointment = baselineExistingAppointment,
          baselineUpdate.copy(
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(16, 35),
            attendanceData = AttendanceDataDto.valid().copy(
              penaltyMinutes = null,
              penaltyTime = HourMinuteDuration(Duration.ofHours(6).plusMinutes(30)),
            ),
          ),
        )
      }

      @Test
      fun `if penalty minutes is less than duration, do nothing`() {
        service.validateUpdate(
          appointment = baselineExistingAppointment,
          baselineUpdate.copy(
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(16, 35),
            attendanceData = AttendanceDataDto.valid().copy(
              penaltyMinutes = 390,
              penaltyTime = null,
            ),
          ),
        )
      }

      @Test
      fun `if penalty time is same as duration, do nothing`() {
        service.validateUpdate(
          appointment = baselineExistingAppointment,
          baselineUpdate.copy(
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(16, 35),
            attendanceData = AttendanceDataDto.valid().copy(
              penaltyMinutes = null,
              penaltyTime = HourMinuteDuration(Duration.ofHours(6).plusMinutes(35)),
            ),
          ),
        )
      }

      @Test
      fun `if penalty minutes is same as duration, do nothing`() {
        service.validateUpdate(
          appointment = baselineExistingAppointment,
          baselineUpdate.copy(
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(16, 35),
            attendanceData = AttendanceDataDto.valid().copy(
              penaltyMinutes = 395,
              penaltyTime = HourMinuteDuration(Duration.ofHours(6).plusMinutes(35)),
            ),
          ),
        )
      }

      @Test
      fun `if penalty time is greater than as duration, throw exception`() {
        assertThatThrownBy {
          service.validateUpdate(
            appointment = baselineExistingAppointment,
            baselineUpdate.copy(
              startTime = LocalTime.of(10, 0),
              endTime = LocalTime.of(16, 35),
              attendanceData = AttendanceDataDto.valid().copy(
                penaltyMinutes = null,
                penaltyTime = HourMinuteDuration(Duration.ofHours(6).plusMinutes(36)),
              ),
            ),
          )
        }.isInstanceOf(BadRequestException::class.java)
          .hasMessage("Penalty duration '6 hours 36 minutes' is greater than appointment duration '6 hours 35 minutes'")
      }

      @Test
      fun `if penalty minutes is greater than as duration, throw exception`() {
        assertThatThrownBy {
          service.validateUpdate(
            appointment = baselineExistingAppointment,
            baselineUpdate.copy(
              startTime = LocalTime.of(10, 0),
              endTime = LocalTime.of(16, 35),
              attendanceData = AttendanceDataDto.valid().copy(
                penaltyMinutes = 396,
                penaltyTime = HourMinuteDuration(Duration.ofMinutes(5)),
              ),
            ),
          )
        }.isInstanceOf(BadRequestException::class.java)
          .hasMessage("Penalty duration '6 hours 36 minutes' is greater than appointment duration '6 hours 35 minutes'")
      }
    }

    @Nested
    inner class NotesValidation {

      @BeforeEach
      fun setupOutcome() {
        every { contactOutcomeEntityRepository.findByCode(baselineOutcome.code) } returns baselineOutcome
      }

      @Test
      fun `null notes is accepted`() {
        assertThatCode {
          service.validateUpdate(
            appointment = baselineExistingAppointment,
            baselineUpdate.copy(notes = null),
          )
        }.doesNotThrowAnyException()
      }

      @Test
      fun `empty notes is accepted`() {
        assertThatCode {
          service.validateUpdate(
            appointment = baselineExistingAppointment,
            baselineUpdate.copy(notes = ""),
          )
        }.doesNotThrowAnyException()
      }

      @Test
      fun `notes length 4000 is accepted`() {
        val notes = "a".repeat(4000)
        assertThatCode {
          service.validateUpdate(
            appointment = baselineExistingAppointment,
            baselineUpdate.copy(notes = notes),
          )
        }.doesNotThrowAnyException()
      }

      @Test
      fun `notes length 4001 throws BadRequest`() {
        val notes = "a".repeat(4001)
        assertThatThrownBy {
          service.validateUpdate(
            appointment = baselineExistingAppointment,
            baselineUpdate.copy(notes = notes),
          )
        }.isInstanceOf(BadRequestException::class.java)
          .hasMessage("Notes must be fewer than 4000 characters")
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

        service.validateUpdate(
          appointment = baselineExistingAppointment,
          update = baselineUpdate,
        )
      }

      @Test
      fun `ETE project with sufficient remaining ETE allowance for appointment`() {
        every { projectService.getProject(PROJECT_CODE) } returns eteProject
        every { appointmentCalculationService.minutesToCredit(any(), any(), any(), any()) } returns Duration.ofMinutes(60)
        every { offenderService.getUnpaidWorkDetails(CRN, EVENT_NUMBER) } returns baselineUnpaidWorkDetails.copy(
          remainingEteMinutes = 60,
        )

        service.validateUpdate(
          appointment = baselineExistingAppointment.copy(
            minutesCredited = null,
          ),
          update = baselineUpdate,
        )
      }

      @Test
      fun `ETE project with insufficient remaining ETE allowance for appointment, throw exception`() {
        every { projectService.getProject(PROJECT_CODE) } returns eteProject
        every { appointmentCalculationService.minutesToCredit(any(), any(), any(), any()) } returns Duration.ofMinutes(61)
        every { offenderService.getUnpaidWorkDetails(CRN, EVENT_NUMBER) } returns baselineUnpaidWorkDetails.copy(
          remainingEteMinutes = 60,
        )

        assertThatThrownBy {
          service.validateUpdate(
            appointment = baselineExistingAppointment.copy(
              minutesCredited = null,
            ),
            update = baselineUpdate,
          )
        }.isInstanceOf(BadRequestException::class.java)
          .hasMessage("Credited minutes of '1 hours 1 minutes' exceeds remaining allowed ETE time of '1 hours 0 minutes'")
      }

      @Test
      fun `Ensure existing minutes credited aren't 'double counted' when updating minutes credited`() {
        every { projectService.getProject(PROJECT_CODE) } returns eteProject
        every { appointmentCalculationService.minutesToCredit(any(), any(), any(), any()) } returns Duration.ofMinutes(120)
        every { offenderService.getUnpaidWorkDetails(CRN, EVENT_NUMBER) } returns baselineUnpaidWorkDetails.copy(
          remainingEteMinutes = 20,
        )

        service.validateUpdate(
          appointment = baselineExistingAppointment.copy(
            minutesCredited = 100,
          ),
          update = baselineUpdate,
        )
      }
    }
  }
}
