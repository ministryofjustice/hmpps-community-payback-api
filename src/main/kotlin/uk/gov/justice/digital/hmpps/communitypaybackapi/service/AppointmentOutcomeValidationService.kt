package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.BadRequestException
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EnforcementActionEntityRepository

@Service
class AppointmentOutcomeValidationService(
  private val contactOutcomeEntityRepository: ContactOutcomeEntityRepository,
  private val enforcementActionEntityRepository: EnforcementActionEntityRepository,
) {

  @SuppressWarnings("ThrowsCount")
  fun validate(outcome: UpdateAppointmentOutcomeDto) {
    val contactOutcome = contactOutcomeEntityRepository.findByIdOrNull(outcome.contactOutcomeId) ?: throw BadRequestException("Contact outcome not found for ID " + outcome.contactOutcomeId.toString())

    if (contactOutcome.enforceable) {
      val enforcementDto = requireNotNull(outcome.enforcementData) {
        "Enforcement data is required for enforceable contact outcomes"
      }

      val enforcementActionId = requireNotNull(enforcementDto.enforcementActionId) {
        "Enforcement action ID is required for enforceable contact outcomes"
      }

      val enforcement = enforcementActionEntityRepository.findById(enforcementActionId)
        .orElseThrow { BadRequestException("Enforcement action not found for ID $enforcementActionId") }
      if (enforcement.respondByDateRequired) {
        require(outcome.enforcementData.respondBy != null) {
          "Respond by date is required for enforceable contact outcomes"
        }
      }
    }
  }
}
