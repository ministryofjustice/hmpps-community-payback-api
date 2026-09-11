package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.validation.ValidationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.validation.ValidatorWithContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionResolutionDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionResolutionTypeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventResolutionEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.EteMappers
import java.util.UUID

@Service
class EteValidationService(
  private val contactOutcomeEntityRepository: ContactOutcomeEntityRepository,
  private val eteMapper: EteMappers,
) : ValidatorWithContext<CourseCompletionResolutionDto, EteValidationService.CourseCompletionValidationContext>() {

  class CourseCompletionValidationContext(
    val courseCompletionEvent: EteCourseCompletionEventEntity,
  ) : ValidationContext<CourseCompletionResolutionDto> {
    val existingResolutionEntity: EteCourseCompletionEventResolutionEntity?
      get() = courseCompletionEvent.resolution

    var proposedResolutionEntity: EteCourseCompletionEventResolutionEntity? = null

    var contactOutcomeEntity: ContactOutcomeEntity? = null
  }

  override fun configureContext(value: CourseCompletionResolutionDto, ctx: CourseCompletionValidationContext): CourseCompletionValidationContext {
    ctx.contactOutcomeEntity = value.creditTimeDetails?.contactOutcomeCode?.let { contactOutcomeEntityRepository.findByCode(it) }

    if (value.type == CourseCompletionResolutionTypeDto.CREDIT_TIME) {
      ctx.proposedResolutionEntity = eteMapper.toResolutionEntityForCreditTime(
        id = UUID.randomUUID(),
        courseCompletionEvent = ctx.courseCompletionEvent,
        courseCompletionResolution = value,
        // setting to 0L is fine here because isLogicallyIdentical() only checks this value when
        // the resolution indicates that an existing appointment is being updated
        deliusAppointmentId = value.creditTimeDetails?.appointmentIdToUpdate ?: 0L,
      )
    }

    return ctx
  }

  override fun configureRules() {
    rule {
      assume { value -> value.type == CourseCompletionResolutionTypeDto.CREDIT_TIME }
      expect { value -> value.crn != null }
      otherwise {
        this isError "CREDIT_TIME_NEEDS_CRN"
        field = "$.crn"
      }
    }

    rule {
      assume { value -> value.type == CourseCompletionResolutionTypeDto.CREDIT_TIME }
      expect { value -> value.creditTimeDetails != null }
      otherwise {
        this isError "CREDIT_TIME_NEEDS_DETAILS"
        field = "$.creditTimeDetails"
      }
    }

    rule {
      assume { value -> value.type == CourseCompletionResolutionTypeDto.CREDIT_TIME }
      assume { value -> value.creditTimeDetails != null }
      expect { _, ctx -> ctx.contactOutcomeEntity != null }
      otherwise {
        this isError "UNKNOWN_CONTACT_OUTCOME"
        field = "$.creditTimeDetails.contactOutcomeCode"
        data {
          "code" to { value -> value.creditTimeDetails!!.contactOutcomeCode }
        }
      }
    }

    rule {
      assume { value -> value.type == CourseCompletionResolutionTypeDto.DONT_CREDIT_TIME }
      expect { value -> value.dontCreditTimeDetails != null }
      otherwise {
        this isError "DONT_CREDIT_TIME_NEEDS_DETAILS"
        field = "$.dontCreditTimeDetails"
      }
    }

    rule {
      assume { _, ctx -> ctx.existingResolutionEntity != null }
      assume { _, ctx -> ctx.proposedResolutionEntity != null }
      expect { _, ctx -> ctx.existingResolutionEntity!!.isLogicallyIdentical(ctx.proposedResolutionEntity!!) }
      otherwise {
        this isError "RESOLUTION_ALREADY_EXISTS"
        field = "$.creditTimeDetails"
        data {
          "id" to { _, ctx -> ctx.existingResolutionEntity!!.id }
        }
      }
    }

    rule {
      assume { _, ctx -> ctx.existingResolutionEntity != null }
      assume { _, ctx -> ctx.proposedResolutionEntity != null }
      expect { _, ctx -> !ctx.existingResolutionEntity!!.isLogicallyIdentical(ctx.proposedResolutionEntity!!) }
      otherwise {
        this isWarning "EXISTING_IDENTICAL_RESOLUTION"
        field = "$.creditTimeDetails"
      }
    }
  }
}
