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

      assertThatThrownBy { service.validate(outcome(contactOutcomeId = id)) }
        .isInstanceOf(BadRequestException::class.java)
        .hasMessage("Contact outcome not found for ID $id")
    }
  }

  @Nested
  inner class NonEnforceableOutcome {
    @Test
    fun `does not require enforcement data when contact outcome is non-enforceable`() {
      val contact = ContactOutcomeEntity.valid().copy(enforceable = false)
      every { contactOutcomeEntityRepository.findById(contact.id) } returns Optional.of(contact)

      assertThatCode { service.validate(outcome(contactOutcomeId = contact.id, enforcementData = null)) }

      verify(exactly = 0) { enforcementActionEntityRepository.findById(any()) }
    }
  }

  @Nested
  inner class EnforceableOutcomeBasics {
    @Test
    fun `requires enforcementData when contact outcome is enforceable`() {
      val contact = ContactOutcomeEntity.valid().copy(enforceable = true)
      every { contactOutcomeEntityRepository.findById(contact.id) } returns Optional.of(contact)

      assertThatThrownBy { service.validate(outcome(contactOutcomeId = contact.id, enforcementData = null)) }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Enforcement data is required for enforceable contact outcomes")
    }

    @Test
    fun `requires enforcementActionId when contact outcome is enforceable`() {
      val contact = ContactOutcomeEntity.valid().copy(enforceable = true)
      every { contactOutcomeEntityRepository.findById(contact.id) } returns Optional.of(contact)

      val enforcement = EnforcementDto(enforcementActionId = null, respondBy = LocalDate.now())

      assertThatThrownBy { service.validate(outcome(contactOutcomeId = contact.id, enforcementData = enforcement)) }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Enforcement action ID is required for enforceable contact outcomes")
    }
  }

  @Nested
  inner class EnforcementActionLookup {
    @Test
    fun `throws BadRequest when enforcement action not found`() {
      val contact = ContactOutcomeEntity.valid().copy(enforceable = true)
      val enforcementId = UUID.randomUUID()
      every { contactOutcomeEntityRepository.findById(contact.id) } returns Optional.of(contact)
      every { enforcementActionEntityRepository.findById(enforcementId) } returns Optional.empty()

      val enforcement = EnforcementDto(enforcementActionId = enforcementId, respondBy = LocalDate.now())

      assertThatThrownBy { service.validate(outcome(contactOutcomeId = contact.id, enforcementData = enforcement)) }
        .isInstanceOf(BadRequestException::class.java)
        .hasMessage("Enforcement action not found for ID $enforcementId")
    }
  }

  @Nested
  inner class RespondByRequirementFromAction {
    @Test
    fun `passes when enforcement action does not require respondBy`() {
      val contact = ContactOutcomeEntity.valid().copy(enforceable = true)
      val enforcement = EnforcementActionEntity.valid().copy(respondByDateRequired = false)
      every { contactOutcomeEntityRepository.findById(contact.id) } returns Optional.of(contact)
      every { enforcementActionEntityRepository.findById(enforcement.id) } returns Optional.of(enforcement)

      val dto = EnforcementDto(enforcementActionId = enforcement.id, respondBy = LocalDate.now())

      assertThatCode { service.validate(outcome(contactOutcomeId = contact.id, enforcementData = dto)) }
        .doesNotThrowAnyException()
    }

    @Test
    fun `requires respondBy when enforcement action requires it`() {
      val contact = ContactOutcomeEntity.valid().copy(enforceable = true)
      val enforcement = EnforcementActionEntity.valid().copy(respondByDateRequired = true)
      every { contactOutcomeEntityRepository.findById(contact.id) } returns Optional.of(contact)
      every { enforcementActionEntityRepository.findById(enforcement.id) } returns Optional.of(enforcement)

      val dtoMissingRespondBy = EnforcementDto(enforcementActionId = enforcement.id, respondBy = null)

      assertThatThrownBy { service.validate(outcome(contactOutcomeId = contact.id, enforcementData = dtoMissingRespondBy)) }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Respond by date is required for enforceable contact outcomes")
    }
  }
}
