package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionCreditTimeDetailsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionResolutionDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionResolutionTypeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventResolutionEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.EteValidationService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.EteValidationService.ValidationResult
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.EteMappers

@ExtendWith(MockKExtension::class)
class EteValidationServiceTest {

  @RelaxedMockK
  lateinit var contactOutcomeEntityRepository: ContactOutcomeEntityRepository

  @RelaxedMockK
  lateinit var eteMappers: EteMappers

  @InjectMockKs
  private lateinit var eteValidationService: EteValidationService

  companion object {
    const val CONTACT_OUTCOME_CODE = "CTC01"
  }

  @Nested
  inner class ValidateCourseCompletionResolution {

    @Nested
    inner class CreditTime {

      val baselineCourseCompletionResolution = CourseCompletionResolutionDto.valid().copy(
        type = CourseCompletionResolutionTypeDto.CREDIT_TIME,
        creditTimeDetails = CourseCompletionCreditTimeDetailsDto.valid().copy(
          contactOutcomeCode = CONTACT_OUTCOME_CODE,
        ),
      )

      val baselineCourseCompletionEvent = EteCourseCompletionEventEntity.valid().copy(
        resolution = null,
      )

      @BeforeEach
      fun baselineMocks() {
        every {
          contactOutcomeEntityRepository.findByCode(CONTACT_OUTCOME_CODE)
        } returns ContactOutcomeEntity.valid()
      }

      @Test
      fun success() {
        eteValidationService.validateCourseCompletionResolution(
          baselineCourseCompletionResolution,
          baselineCourseCompletionEvent,
        )
      }

      @Test
      fun `error if credit time details not provided`() {
        assertThatThrownBy {
          eteValidationService.validateCourseCompletionResolution(
            baselineCourseCompletionResolution.copy(
              creditTimeDetails = null,
            ),
            baselineCourseCompletionEvent,
          )
        }.hasMessage("Credit Time Details are required for type CREDIT_TIME")
      }

      @Test
      fun `error if invalid contact outcome code`() {
        every {
          contactOutcomeEntityRepository.findByCode(CONTACT_OUTCOME_CODE)
        } returns null

        assertThatThrownBy {
          eteValidationService.validateCourseCompletionResolution(
            baselineCourseCompletionResolution,
            baselineCourseCompletionEvent,
          )
        }.hasMessage("Cannot find contact outcome with code CTC01")
      }
    }

    @Nested
    inner class CourseAlreadyCompletedWithinThreshold {

      val baselineCourseCompletionResolution = CourseCompletionResolutionDto.valid().copy(
        type = CourseCompletionResolutionTypeDto.COURSE_ALREADY_COMPLETED_WITHIN_THRESHOLD,
        creditTimeDetails = null,
      )

      val baselineCourseCompletionEvent = EteCourseCompletionEventEntity.valid().copy(
        resolution = null,
      )

      @Test
      fun success() {
        eteValidationService.validateCourseCompletionResolution(
          baselineCourseCompletionResolution,
          baselineCourseCompletionEvent,
        )
      }

      @Test
      fun `error if credit time details are provided`() {
        assertThatThrownBy {
          eteValidationService.validateCourseCompletionResolution(
            baselineCourseCompletionResolution.copy(
              creditTimeDetails = CourseCompletionCreditTimeDetailsDto.valid(),
            ),
            baselineCourseCompletionEvent,
          )
        }.hasMessage("Credit Time Details should not be provided for type COURSE_ALREADY_COMPLETED_WITHIN_THRESHOLD")
      }
    }

    @Nested
    inner class ExistingResolution {

      @BeforeEach
      fun baselineMocks() {
        every {
          contactOutcomeEntityRepository.findByCode(CONTACT_OUTCOME_CODE)
        } returns ContactOutcomeEntity.valid()
      }

      val baselineCourseCompletionOutcome = CourseCompletionResolutionDto.valid().copy(
        type = CourseCompletionResolutionTypeDto.CREDIT_TIME,
        creditTimeDetails = CourseCompletionCreditTimeDetailsDto.valid().copy(
          contactOutcomeCode = CONTACT_OUTCOME_CODE,
        ),
      )

      val baselineCourseCompletionEvent = EteCourseCompletionEventEntity.valid().copy(
        resolution = null,
      )

      @Test
      fun `if no existing resolution, is valid`() {
        eteValidationService.validateCourseCompletionResolution(
          baselineCourseCompletionOutcome,
          baselineCourseCompletionEvent.copy(
            resolution = null,
          ),
        )
      }

      @Test
      fun `if existing resolution is logically identical, return EXISTING_IDENTICAL_RESOLUTION`() {
        val courseCompletionEvent = baselineCourseCompletionEvent.copy(
          resolution = EteCourseCompletionEventResolutionEntity.valid(),
        )

        every {
          eteMappers.toResolutionEntityForCreditTime(
            id = any(),
            courseCompletionEvent = courseCompletionEvent,
            courseCompletionResolution = baselineCourseCompletionOutcome,
            deliusAppointmentId = baselineCourseCompletionOutcome.creditTimeDetails!!.appointmentIdToUpdate!!,
          )
        } returns courseCompletionEvent.resolution!!.copy()

        val result = eteValidationService.validateCourseCompletionResolution(
          resolution = baselineCourseCompletionOutcome,
          courseCompletionEvent = courseCompletionEvent,
        )

        assertThat(result).isEqualTo(ValidationResult.EXISTING_IDENTICAL_RESOLUTION)
      }

      @Test
      fun `if existing resolution is not logically identical, error`() {
        val courseCompletionEvent = baselineCourseCompletionEvent.copy(
          resolution = EteCourseCompletionEventResolutionEntity.valid(),
        )

        every {
          eteMappers.toResolutionEntityForCreditTime(
            id = any(),
            courseCompletionEvent = courseCompletionEvent,
            courseCompletionResolution = baselineCourseCompletionOutcome,
            deliusAppointmentId = baselineCourseCompletionOutcome.creditTimeDetails!!.appointmentIdToUpdate!!,
          )
        } returns courseCompletionEvent.resolution!!.copy(
          projectCode = "some other project code",
        )

        assertThatThrownBy {
          eteValidationService.validateCourseCompletionResolution(
            resolution = baselineCourseCompletionOutcome,
            courseCompletionEvent = courseCompletionEvent,
          )
        }.hasMessage("A resolution has already been defined for this course completion record")
      }
    }
  }
}
