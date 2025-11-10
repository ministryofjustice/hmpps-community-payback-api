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
import java.time.LocalDate
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
}
