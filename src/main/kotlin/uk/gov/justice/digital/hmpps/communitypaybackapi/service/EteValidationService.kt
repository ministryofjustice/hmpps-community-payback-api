package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.BadRequestException
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository

@Service
class EteValidationService(
  private val contactOutcomeEntityRepository: ContactOutcomeEntityRepository,
) {

  fun validateCourseCompletionOutcome(
    outcome: CourseCompletionOutcomeDto,
  ) {
    val contactOutcomeCode = outcome.contactOutcomeCode
    if (contactOutcomeEntityRepository.findByCode(outcome.contactOutcomeCode) == null) {
      throw BadRequestException("Cannot find contact outcome with code $contactOutcomeCode")
    }
  }
}
