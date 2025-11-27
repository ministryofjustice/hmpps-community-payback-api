package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.HourMinuteDuration
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AttendanceDataDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.BadRequestException
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.EnforcementDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentOutcomeValidationService
import java.time.Duration
import java.time.LocalTime
import java.util.UUID

@ExtendWith(MockKExtension::class)
class AppointmentOutcomeValidationServiceTest {

  @MockK
  lateinit var contactOutcomeEntityRepository: ContactOutcomeEntityRepository

  @InjectMockKs
  lateinit var service: AppointmentOutcomeValidationService

  private fun outcome(
    contactOutcomeCode: String = "OUTCOME1",
    enforcementData: EnforcementDto? = null,
  ): UpdateAppointmentOutcomeDto = UpdateAppointmentOutcomeDto.valid(
    contactOutcomeCode = contactOutcomeCode,
    enforcementActionId = enforcementData?.enforcementActionId ?: UUID.randomUUID(),
  ).copy(
    enforcementData = enforcementData,
  )

  @Nested
  inner class ContactOutcomeLookup {
    @Test
    fun `throws BadRequestException when contact outcome not found`() {
      val code = "MISSING_CODE"
      every { contactOutcomeEntityRepository.findByCode(code) } returns null

      assertThatThrownBy { service.validateContactOutcome(outcome(contactOutcomeCode = code)) }
        .isInstanceOf(BadRequestException::class.java)
        .hasMessage("Contact outcome not found for code $code")
    }
  }

  @Nested
  inner class ContactOutcomeAttended {

    @Test
    fun `if outcome attended is false, attendance data isn't required`() {
      val outcome = ContactOutcomeEntity.valid().copy(attended = false, enforceable = false)
      every { contactOutcomeEntityRepository.findByCode(outcome.code) } returns outcome

      service.validateContactOutcome(UpdateAppointmentOutcomeDto.valid().copy(contactOutcomeCode = outcome.code))
    }

    @Test
    fun `if outcome attended is true and attendance data isn't provided, throw exception`() {
      val outcome = ContactOutcomeEntity.valid().copy(attended = true, enforceable = false)
      every { contactOutcomeEntityRepository.findByCode(outcome.code) } returns outcome

      assertThatThrownBy {
        service.validateContactOutcome(
          UpdateAppointmentOutcomeDto.valid().copy(
            contactOutcomeCode = outcome.code,
            attendanceData = null,
          ),
        )
      }.isInstanceOf(BadRequestException::class.java)
        .hasMessage("Attendance data is required for 'attended' contact outcomes")
    }

    @Test
    fun `if outcome attended is true and attendance data is provided, don't throw exception`() {
      val outcome = ContactOutcomeEntity.valid().copy(attended = true, enforceable = false)
      every { contactOutcomeEntityRepository.findByCode(outcome.code) } returns outcome

      service.validateContactOutcome(
        UpdateAppointmentOutcomeDto.valid().copy(
          contactOutcomeCode = outcome.code,
          attendanceData = AttendanceDataDto.valid(),
        ),
      )
    }
  }

  @Nested
  inner class AppointmentDuration {

    @Test
    fun `if end time same as start time, throw exception`() {
      assertThatThrownBy {
        service.validateDuration(
          UpdateAppointmentOutcomeDto.valid().copy(
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(10, 0),
          ),
        )
      }.isInstanceOf(BadRequestException::class.java)
        .hasMessage("End Time '10:00' must be after Start Time '10:00'")
    }

    @Test
    fun `if end time after start time, do nothing`() {
      service.validateDuration(
        UpdateAppointmentOutcomeDto.valid().copy(
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(10, 1),
        ),
      )
    }

    @Test
    fun `if end time before start time, throw exception`() {
      assertThatThrownBy {
        service.validateDuration(
          UpdateAppointmentOutcomeDto.valid().copy(
            startTime = LocalTime.of(10, 1),
            endTime = LocalTime.of(10, 0),
          ),
        )
      }.isInstanceOf(BadRequestException::class.java)
        .hasMessage("End Time '10:00' must be after Start Time '10:01'")
    }
  }

  @Nested
  inner class PenaltyTimeDuration {

    @Test
    fun `if no penalty time, do nothing`() {
      service.validatePenaltyTime(
        UpdateAppointmentOutcomeDto.valid().copy(
          attendanceData = AttendanceDataDto.valid().copy(
            penaltyTime = null,
          ),
        ),
      )
    }

    @Test
    fun `if penalty time is less than duration, do nothing`() {
      service.validatePenaltyTime(
        UpdateAppointmentOutcomeDto.valid().copy(
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(16, 35),
          attendanceData = AttendanceDataDto.valid().copy(
            penaltyTime = HourMinuteDuration(Duration.ofHours(6).plusMinutes(30)),
          ),
        ),
      )
    }

    @Test
    fun `if penalty time is same as duration, do nothing`() {
      service.validatePenaltyTime(
        UpdateAppointmentOutcomeDto.valid().copy(
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(16, 35),
          attendanceData = AttendanceDataDto.valid().copy(
            penaltyTime = HourMinuteDuration(Duration.ofHours(6).plusMinutes(35)),
          ),
        ),
      )
    }

    @Test
    fun `if penalty time is greater than as duration, throw exception`() {
      assertThatThrownBy {
        service.validatePenaltyTime(
          UpdateAppointmentOutcomeDto.valid().copy(
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(16, 35),
            attendanceData = AttendanceDataDto.valid().copy(
              penaltyTime = HourMinuteDuration(Duration.ofHours(6).plusMinutes(36)),
            ),
          ),
        )
      }.isInstanceOf(BadRequestException::class.java)
        .hasMessage("Penalty duration 'PT6H36M' is greater than appointment duration 'PT6H35M'")
    }
  }

  @Nested
  inner class NotesValidation {

    @Test
    fun `null notes is accepted`() {
      assertThatCode {
        service.validateNotes(UpdateAppointmentOutcomeDto.valid().copy(notes = null))
      }.doesNotThrowAnyException()
    }

    @Test
    fun `empty notes is accepted`() {
      assertThatCode {
        service.validateNotes(UpdateAppointmentOutcomeDto.valid().copy(notes = ""))
      }.doesNotThrowAnyException()
    }

    @Test
    fun `notes length 4000 is accepted`() {
      val notes = "a".repeat(4000)
      assertThatCode {
        service.validateNotes(UpdateAppointmentOutcomeDto.valid().copy(notes = notes))
      }.doesNotThrowAnyException()
    }

    @Test
    fun `notes length 4001 throws BadRequest`() {
      val notes = "a".repeat(4001)
      assertThatThrownBy {
        service.validateNotes(UpdateAppointmentOutcomeDto.valid().copy(notes = notes))
      }.isInstanceOf(BadRequestException::class.java)
        .hasMessage("Outcome notes must be fewer than 4000 characters")
    }
  }
}
