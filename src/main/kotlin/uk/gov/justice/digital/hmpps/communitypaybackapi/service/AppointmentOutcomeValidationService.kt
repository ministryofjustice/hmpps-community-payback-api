package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.BadRequestException
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EnforcementActionEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.validateNotNull

@Service
class AppointmentOutcomeValidationService(
  private val contactOutcomeEntityRepository: ContactOutcomeEntityRepository,
  private val enforcementActionEntityRepository: EnforcementActionEntityRepository,
) {

  @SuppressWarnings("ThrowsCount")
  fun validate(outcome: UpdateAppointmentOutcomeDto) {
    val contactOutcome = contactOutcomeEntityRepository.findByIdOrNull(outcome.contactOutcomeId) ?: throw BadRequestException("Contact outcome not found for ID " + outcome.contactOutcomeId.toString())

    if (contactOutcome.enforceable) {
      val enforcementDto = validateNotNull(outcome.enforcementData) {
        "Enforcement data is required for enforceable contact outcomes"
      }

      val enforcementActionId = enforcementDto.enforcementActionId

      val enforcement = enforcementActionEntityRepository.findById(enforcementActionId)
        .orElseThrow { BadRequestException("Enforcement action not found for ID $enforcementActionId") }
      if (enforcement.respondByDateRequired) {
        validateNotNull(outcome.enforcementData.respondBy) {
          "Respond by date is required for enforceable contact outcomes"
        }
      }
    }
  }
}
