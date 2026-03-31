package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionResolutionDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionResolutionTypeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.BadRequestException
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.EteMappers
import java.util.UUID

@SuppressWarnings("ThrowsCount")
@Service
class EteValidationService(
  private val contactOutcomeEntityRepository: ContactOutcomeEntityRepository,
  private val eteMapper: EteMappers,
) {
  fun validateCourseCompletionResolution(
    resolution: CourseCompletionResolutionDto,
    courseCompletionEvent: EteCourseCompletionEventEntity,
  ): ValidationResult {
    when (resolution.type) {
      CourseCompletionResolutionTypeDto.CREDIT_TIME -> validateCreditTime(resolution)
      CourseCompletionResolutionTypeDto.DONT_CREDIT_TIME -> validateDontCreditTime(resolution)
    }

    return validateExistingResolution(resolution, courseCompletionEvent)
  }

  private fun validateCreditTime(resolution: CourseCompletionResolutionDto) {
    if (resolution.crn == null) {
      throw BadRequestException("CRN is required for type ${CourseCompletionResolutionTypeDto.CREDIT_TIME}")
    }

    if (resolution.creditTimeDetails == null) {
      throw BadRequestException("Credit Time Details are required for type ${CourseCompletionResolutionTypeDto.CREDIT_TIME}")
    }

    val contactOutcomeCode = resolution.creditTimeDetails.contactOutcomeCode
    if (contactOutcomeEntityRepository.findByCode(resolution.creditTimeDetails.contactOutcomeCode) == null) {
      throw BadRequestException("Cannot find contact outcome with code $contactOutcomeCode")
    }
  }

  private fun validateDontCreditTime(resolution: CourseCompletionResolutionDto) {
    if (resolution.dontCreditTimeDetails == null) {
      throw BadRequestException("Don't Credit Time Details are required for type ${CourseCompletionResolutionTypeDto.DONT_CREDIT_TIME}")
    }
  }

  private fun validateExistingResolution(
    resolution: CourseCompletionResolutionDto,
    courseCompletionEvent: EteCourseCompletionEventEntity,
  ): ValidationResult {
    val existingResolution = courseCompletionEvent.resolution
    if (existingResolution == null) {
      return ValidationResult.VALID
    } else {
      val proposedResolutionEntity = eteMapper.toResolutionEntityForCreditTime(
        id = UUID.randomUUID(),
        courseCompletionEvent = courseCompletionEvent,
        courseCompletionResolution = resolution,
        // setting to 0L is fine here because isLogicallyIdentical() only checks this value when
        // the resolution indicates that an existing appointment is being updated
        deliusAppointmentId = resolution.creditTimeDetails?.appointmentIdToUpdate ?: 0L,
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
