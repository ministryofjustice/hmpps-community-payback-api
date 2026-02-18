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
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.BadRequestException
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentValidationService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.Validated
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

@ExtendWith(MockKExtension::class)
class AppointmentValidationServiceTest {

  @MockK
  lateinit var contactOutcomeEntityRepository: ContactOutcomeEntityRepository

  @InjectMockKs
  lateinit var service: AppointmentValidationService

  companion object {
    const val OUTCOME_CODE = "OUTCOME1"
  }

  @Nested
  inner class Create {

    val baselineCreate = CreateAppointmentDto.valid().copy(
      contactOutcomeCode = OUTCOME_CODE,
      startTime = LocalTime.MIN,
      endTime = LocalTime.MAX,
    )
    val baselineOutcome = ContactOutcomeEntity.valid().copy(code = OUTCOME_CODE)

    @Nested
    inner class Success {

      @Test
      fun success() {
        every { contactOutcomeEntityRepository.findByCode(OUTCOME_CODE) } returns baselineOutcome

        val result = service.validateCreate(
          create = baselineCreate,
        )

        assertThat(result).isEqualTo(Validated(baselineCreate))
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
          .hasMessage("Contact outcome not found for code $OUTCOME_CODE")
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
          .hasMessage("If the appointment is in the future, only acceptable absences are permitted to be recorded")
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
          .hasMessage("If the appointment is in the future, only acceptable absences are permitted to be recorded")
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
          .hasMessage("Attendance data is required for 'attended' contact outcomes")
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

      @BeforeEach
      fun setupOutcome() {
        every { contactOutcomeEntityRepository.findByCode(baselineOutcome.code) } returns baselineOutcome
      }

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
          .hasMessage("Penalty duration 'PT6H36M' is greater than appointment duration 'PT6H35M'")
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
          .hasMessage("Penalty duration 'PT6H36M' is greater than appointment duration 'PT6H35M'")
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
          .hasMessage("Outcome notes must be fewer than 4000 characters")
      }
    }
  }

  @Nested
  inner class Update {

    val baselineUpdate = UpdateAppointmentOutcomeDto.valid().copy(
      contactOutcomeCode = OUTCOME_CODE,
      startTime = LocalTime.MIN,
      endTime = LocalTime.MAX,
    )
    val baselineOutcome = ContactOutcomeEntity.valid().copy(code = OUTCOME_CODE)

    @Nested
    inner class Success {

      @Test
      fun success() {
        every { contactOutcomeEntityRepository.findByCode(OUTCOME_CODE) } returns baselineOutcome

        val result = service.validateUpdate(
          appointment = AppointmentDto.valid(),
          update = baselineUpdate,
        )

        assertThat(result).isEqualTo(Validated(baselineUpdate))
      }
    }

    @Nested
    inner class ContactOutcome {

      @Test
      fun `throws BadRequestException when contact outcome not found`() {
        every { contactOutcomeEntityRepository.findByCode(OUTCOME_CODE) } returns null

        assertThatThrownBy {
          service.validateUpdate(
            appointment = AppointmentDto.valid(),
            update = baselineUpdate,
          )
        }.isInstanceOf(BadRequestException::class.java)
          .hasMessage("Contact outcome not found for code $OUTCOME_CODE")
      }

      @Test
      fun `if appointment is current or in the past, attendance outcome can be recorded`() {
        val outcome = baselineOutcome.copy(attended = true, enforceable = false)
        every { contactOutcomeEntityRepository.findByCode(OUTCOME_CODE) } returns outcome

        service.validateUpdate(
          appointment = AppointmentDto.valid().copy(date = LocalDate.now()),
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
          appointment = AppointmentDto.valid().copy(date = LocalDate.now()),
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
          appointment = AppointmentDto.valid().copy(date = LocalDate.now()),
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
            appointment = AppointmentDto.valid().copy(date = LocalDate.now()),
            update = baselineUpdate.copy(
              startTime = LocalTime.now().plusMinutes(1),
            ),
          )
        }.isInstanceOf(BadRequestException::class.java)
          .hasMessage("If the appointment is in the future, only acceptable absences are permitted to be recorded")
      }

      @Test
      fun `if appointment is in future, enforceable outcome can't be recorded`() {
        val outcome = baselineOutcome.copy(attended = false, enforceable = true)
        every { contactOutcomeEntityRepository.findByCode(outcome.code) } returns outcome

        assertThatThrownBy {
          service.validateUpdate(
            appointment = AppointmentDto.valid().copy(date = LocalDate.now()),
            update = baselineUpdate.copy(
              startTime = LocalTime.now().plusMinutes(1),
            ),
          )
        }.isInstanceOf(BadRequestException::class.java)
          .hasMessage("If the appointment is in the future, only acceptable absences are permitted to be recorded")
      }

      @Test
      fun `if appointment is in future, non-enforceable absence can be recorded`() {
        val outcome = baselineOutcome.copy(attended = false, enforceable = false)
        every { contactOutcomeEntityRepository.findByCode(outcome.code) } returns outcome

        service.validateUpdate(
          appointment = AppointmentDto.valid().copy(date = LocalDate.now()),
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
          appointment = AppointmentDto.valid().copy(date = LocalDate.now()),
          update = baselineUpdate,
        )
      }

      @Test
      fun `if outcome attended is true and attendance data isn't provided, throw exception`() {
        val outcome = baselineOutcome.copy(attended = true, enforceable = false)
        every { contactOutcomeEntityRepository.findByCode(outcome.code) } returns outcome

        assertThatThrownBy {
          service.validateUpdate(
            appointment = AppointmentDto.valid().copy(date = LocalDate.now()),
            update = baselineUpdate.copy(
              attendanceData = null,
            ),
          )
        }.isInstanceOf(BadRequestException::class.java)
          .hasMessage("Attendance data is required for 'attended' contact outcomes")
      }

      @Test
      fun `if outcome attended is true and attendance data is provided, don't throw exception`() {
        val outcome = baselineOutcome.copy(attended = true, enforceable = false)
        every { contactOutcomeEntityRepository.findByCode(outcome.code) } returns outcome

        service.validateUpdate(
          appointment = AppointmentDto.valid().copy(date = LocalDate.now()),
          update = baselineUpdate.copy(
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
          service.validateUpdate(
            appointment = AppointmentDto.valid(),
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
          appointment = AppointmentDto.valid(),
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
            appointment = AppointmentDto.valid(),
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
          appointment = AppointmentDto.valid(),
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
          appointment = AppointmentDto.valid(),
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
          appointment = AppointmentDto.valid(),
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
          appointment = AppointmentDto.valid(),
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
          appointment = AppointmentDto.valid(),
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
            appointment = AppointmentDto.valid(),
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
          .hasMessage("Penalty duration 'PT6H36M' is greater than appointment duration 'PT6H35M'")
      }

      @Test
      fun `if penalty minutes is greater than as duration, throw exception`() {
        assertThatThrownBy {
          service.validateUpdate(
            appointment = AppointmentDto.valid(),
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
          .hasMessage("Penalty duration 'PT6H36M' is greater than appointment duration 'PT6H35M'")
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
            appointment = AppointmentDto.valid(),
            baselineUpdate.copy(notes = null),
          )
        }.doesNotThrowAnyException()
      }

      @Test
      fun `empty notes is accepted`() {
        assertThatCode {
          service.validateUpdate(
            appointment = AppointmentDto.valid(),
            baselineUpdate.copy(notes = ""),
          )
        }.doesNotThrowAnyException()
      }

      @Test
      fun `notes length 4000 is accepted`() {
        val notes = "a".repeat(4000)
        assertThatCode {
          service.validateUpdate(
            appointment = AppointmentDto.valid(),
            baselineUpdate.copy(notes = notes),
          )
        }.doesNotThrowAnyException()
      }

      @Test
      fun `notes length 4001 throws BadRequest`() {
        val notes = "a".repeat(4001)
        assertThatThrownBy {
          service.validateUpdate(
            appointment = AppointmentDto.valid(),
            baselineUpdate.copy(notes = notes),
          )
        }.isInstanceOf(BadRequestException::class.java)
          .hasMessage("Outcome notes must be fewer than 4000 characters")
      }
    }
  }
}
