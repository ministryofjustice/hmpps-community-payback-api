package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
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
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EnforcementActionEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EnforcementActionEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentOutcomeValidationService
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.Optional
import java.util.UUID

@ExtendWith(MockKExtension::class)
class AppointmentOutcomeValidationServiceTest {

  @MockK
  lateinit var contactOutcomeEntityRepository: ContactOutcomeEntityRepository

  @MockK
  lateinit var enforcementActionEntityRepository: EnforcementActionEntityRepository

  @InjectMockKs
  lateinit var service: AppointmentOutcomeValidationService

  private fun outcome(
    contactOutcomeId: UUID = UUID.randomUUID(),
    enforcementData: EnforcementDto? = null,
  ): UpdateAppointmentOutcomeDto = UpdateAppointmentOutcomeDto.valid(
    contactOutcomeId = contactOutcomeId,
    enforcementActionId = enforcementData?.enforcementActionId ?: UUID.randomUUID(),
  ).copy(
    enforcementData = enforcementData,
  )

  @Nested
  inner class ContactOutcomeLookup {
    @Test
    fun `throws BadRequestException when contact outcome not found`() {
      val id = UUID.randomUUID()
      every { contactOutcomeEntityRepository.findById(id) } returns Optional.empty()

      assertThatThrownBy { service.validateContactOutcome(outcome(contactOutcomeId = id)) }
        .isInstanceOf(BadRequestException::class.java)
        .hasMessage("Contact outcome not found for ID $id")
    }
  }

  @Nested
  inner class ContactOutcomeEnforceableValidation {

    @Test
    fun `if contact outcome not enforceable, does not require enforcement data`() {
      val contact = ContactOutcomeEntity.valid().copy(enforceable = false)
      every { contactOutcomeEntityRepository.findById(contact.id) } returns Optional.of(contact)

      assertThatCode { service.validateContactOutcome(outcome(contactOutcomeId = contact.id, enforcementData = null)) }

      verify(exactly = 0) { enforcementActionEntityRepository.findById(any()) }
    }

    @Test
    fun `if contact outcome enforceable, throws BadRequest when enforcement action not found`() {
      val contact = ContactOutcomeEntity.valid().copy(enforceable = true)
      val enforcementId = UUID.randomUUID()
      every { contactOutcomeEntityRepository.findById(contact.id) } returns Optional.of(contact)
      every { enforcementActionEntityRepository.findById(enforcementId) } returns Optional.empty()

      val enforcement = EnforcementDto(enforcementActionId = enforcementId, respondBy = LocalDate.now())

      assertThatThrownBy { service.validateContactOutcome(outcome(contactOutcomeId = contact.id, enforcementData = enforcement)) }
        .isInstanceOf(BadRequestException::class.java)
        .hasMessage("Enforcement action not found for ID $enforcementId")
    }

    @Test
    fun `if enforceable, requires enforcementData`() {
      val contact = ContactOutcomeEntity.valid().copy(enforceable = true)
      every { contactOutcomeEntityRepository.findById(contact.id) } returns Optional.of(contact)

      assertThatThrownBy { service.validateContactOutcome(outcome(contactOutcomeId = contact.id, enforcementData = null)) }
        .isInstanceOf(BadRequestException::class.java)
        .hasMessage("Enforcement data is required for enforceable contact outcomes")
    }

    @Test
    fun `if enforceable, but respond by not required, respond by is optional`() {
      val contact = ContactOutcomeEntity.valid().copy(enforceable = true)
      val enforcement = EnforcementActionEntity.valid().copy(respondByDateRequired = false)
      every { contactOutcomeEntityRepository.findById(contact.id) } returns Optional.of(contact)
      every { enforcementActionEntityRepository.findById(enforcement.id) } returns Optional.of(enforcement)

      val dto = EnforcementDto(enforcementActionId = enforcement.id, respondBy = LocalDate.now())

      assertThatCode { service.validateContactOutcome(outcome(contactOutcomeId = contact.id, enforcementData = dto)) }
        .doesNotThrowAnyException()
    }

    @Test
    fun `if enforceable and respond by required, error if respond by not provided`() {
      val contact = ContactOutcomeEntity.valid().copy(enforceable = true)
      val enforcement = EnforcementActionEntity.valid().copy(respondByDateRequired = true)
      every { contactOutcomeEntityRepository.findById(contact.id) } returns Optional.of(contact)
      every { enforcementActionEntityRepository.findById(enforcement.id) } returns Optional.of(enforcement)

      val dtoMissingRespondBy = EnforcementDto(enforcementActionId = enforcement.id, respondBy = null)

      assertThatThrownBy { service.validateContactOutcome(outcome(contactOutcomeId = contact.id, enforcementData = dtoMissingRespondBy)) }
        .isInstanceOf(BadRequestException::class.java)
        .hasMessage("Respond by date is required for enforceable contact outcomes")
    }

    @Test
    fun `if provided, respond by in the past throws BadRequest`() {
      val contact = ContactOutcomeEntity.valid().copy(enforceable = true)
      val enforcement = EnforcementActionEntity.valid().copy(respondByDateRequired = false)
      every { contactOutcomeEntityRepository.findById(contact.id) } returns Optional.of(contact)
      every { enforcementActionEntityRepository.findById(enforcement.id) } returns Optional.of(enforcement)

      val past = LocalDate.now().minusDays(1)
      val dto = EnforcementDto(enforcementActionId = enforcement.id, respondBy = past)

      assertThatThrownBy { service.validateContactOutcome(outcome(contactOutcomeId = contact.id, enforcementData = dto)) }
        .isInstanceOf(BadRequestException::class.java)
        .hasMessage("Respond by date '$past' must be today or in the future")
    }

    @Test
    fun `if provided, respond by today is accepted`() {
      val contact = ContactOutcomeEntity.valid().copy(enforceable = true)
      val enforcement = EnforcementActionEntity.valid().copy(respondByDateRequired = false)
      every { contactOutcomeEntityRepository.findById(contact.id) } returns Optional.of(contact)
      every { enforcementActionEntityRepository.findById(enforcement.id) } returns Optional.of(enforcement)

      val today = LocalDate.now()
      val dto = EnforcementDto(enforcementActionId = enforcement.id, respondBy = today)

      assertThatCode { service.validateContactOutcome(outcome(contactOutcomeId = contact.id, enforcementData = dto)) }
        .doesNotThrowAnyException()
    }

    @Test
    fun `if provided, respond by in the future is accepted`() {
      val contact = ContactOutcomeEntity.valid().copy(enforceable = true)
      val enforcement = EnforcementActionEntity.valid().copy(respondByDateRequired = false)
      every { contactOutcomeEntityRepository.findById(contact.id) } returns Optional.of(contact)
      every { enforcementActionEntityRepository.findById(enforcement.id) } returns Optional.of(enforcement)

      val future = LocalDate.now().plusDays(1)
      val dto = EnforcementDto(enforcementActionId = enforcement.id, respondBy = future)

      assertThatCode { service.validateContactOutcome(outcome(contactOutcomeId = contact.id, enforcementData = dto)) }
        .doesNotThrowAnyException()
    }
  }

  @Nested
  inner class ContactOutcomeAttended {

    @Test
    fun `if outcome attended is false, attendance data isn't required`() {
      val outcome = ContactOutcomeEntity.valid().copy(attended = false, enforceable = false)
      every { contactOutcomeEntityRepository.findById(outcome.id) } returns Optional.of(outcome)

      service.validateContactOutcome(UpdateAppointmentOutcomeDto.valid().copy(contactOutcomeId = outcome.id))
    }

    @Test
    fun `if outcome attended is true and attendance data isn't provided, throw exception`() {
      val outcome = ContactOutcomeEntity.valid().copy(attended = true, enforceable = false)
      every { contactOutcomeEntityRepository.findById(outcome.id) } returns Optional.of(outcome)

      assertThatThrownBy {
        service.validateContactOutcome(
          UpdateAppointmentOutcomeDto.valid().copy(
            contactOutcomeId = outcome.id,
            attendanceData = null,
          ),
        )
      }.isInstanceOf(BadRequestException::class.java)
        .hasMessage("Attendance data is required for 'attended' contact outcomes")
    }

    @Test
    fun `if outcome attended is true and attendance data is provided, don't throw exception`() {
      val outcome = ContactOutcomeEntity.valid().copy(attended = true, enforceable = false)
      every { contactOutcomeEntityRepository.findById(outcome.id) } returns Optional.of(outcome)

      service.validateContactOutcome(
        UpdateAppointmentOutcomeDto.valid().copy(
          contactOutcomeId = outcome.id,
          attendanceData = AttendanceDataDto.valid(),
        ),
      )
    }
  }

  @Nested
  inner class AppointmentDuration {

    @Test
    fun `if end time same as start time, do nothing`() {
      service.validateDuration(
        UpdateAppointmentOutcomeDto.valid().copy(
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(10, 0),
        ),
      )
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
        .hasMessage("End Time '10:00' is before Start Time '10:01'")
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
