package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.BadRequestException
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.EteMappers
import java.util.UUID

@Service
class EteValidationService(
  private val contactOutcomeEntityRepository: ContactOutcomeEntityRepository,
  private val eteMapper: EteMappers,
) {
  fun validateCourseCompletionOutcome(
    outcome: CourseCompletionOutcomeDto,
    courseCompletionEvent: EteCourseCompletionEventEntity,
  ): ValidationResult {
    validateContactOutcomeCode(outcome)
    return validateExistingResolution(outcome, courseCompletionEvent)
  }

  private fun validateContactOutcomeCode(
    outcome: CourseCompletionOutcomeDto,
  ) {
    val contactOutcomeCode = outcome.contactOutcomeCode
    if (contactOutcomeEntityRepository.findByCode(outcome.contactOutcomeCode) == null) {
      throw BadRequestException("Cannot find contact outcome with code $contactOutcomeCode")
    }
  }

  private fun validateExistingResolution(
    outcome: CourseCompletionOutcomeDto,
    courseCompletionEvent: EteCourseCompletionEventEntity,
  ): ValidationResult {
    val existingResolution = courseCompletionEvent.resolution
    if (existingResolution == null) {
      return ValidationResult.VALID
    } else {
      val proposedResolutionEntity = eteMapper.toResolutionEntity(
        id = UUID.randomUUID(),
        courseCompletionEvent = courseCompletionEvent,
        courseCompletionOutcome = outcome,
        // setting to 0L is fine here because isLogicallyIdentical() only checks this value when
        // the resolution indicates that an existing appointment is being updated
        deliusAppointmentId = outcome.appointmentIdToUpdate ?: 0L,
      )

      if (existingResolution.isLogicallyIdentical(proposedResolutionEntity)) {
        return ValidationResult.EXISTING_IDENTICAL_RESOLUTION
      } else {
        throw BadRequestException("A resolution has already been defined for this course completion record")
      }
    }
  }

  enum class ValidationResult {
    VALID,
    EXISTING_IDENTICAL_RESOLUTION,
  }
}
