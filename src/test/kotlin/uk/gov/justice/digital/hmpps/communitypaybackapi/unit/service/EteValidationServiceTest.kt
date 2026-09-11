package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.validation.ValidationResultItem
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionCreditTimeDetailsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionDontCreditTimeDetailsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionResolutionDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionResolutionTypeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventResolutionEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.EteValidationService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.EteValidationService.CourseCompletionValidationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.EteMappers
import uk.gov.justice.digital.hmpps.communitypaybackapi.unit.common.validation.hasErrors
import uk.gov.justice.digital.hmpps.communitypaybackapi.unit.common.validation.hasNoErrors
import uk.gov.justice.digital.hmpps.communitypaybackapi.unit.common.validation.hasNoWarnings
import uk.gov.justice.digital.hmpps.communitypaybackapi.unit.common.validation.hasWarnings

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
  inner class Validate {

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
        val result = eteValidationService.validate(
          baselineCourseCompletionResolution,
          CourseCompletionValidationContext(baselineCourseCompletionEvent),
        )

        assertThat(result).hasNoErrors()
        assertThat(result).hasNoWarnings()
      }

      @Test
      fun `error if crn not provided`() {
        val expectedErrors = listOf(
          ValidationResultItem("$.crn", "CREDIT_TIME_NEEDS_CRN", emptyMap()),
        )

        val result = eteValidationService.validate(
          baselineCourseCompletionResolution.copy(
            crn = null,
          ),
          CourseCompletionValidationContext(baselineCourseCompletionEvent),
        )

        assertThat(result).hasErrors(expectedErrors)
        assertThat(result).hasNoWarnings()
      }

      @Test
      fun `error if credit time details not provided`() {
        val expectedErrors = listOf(
          ValidationResultItem("$.creditTimeDetails", "CREDIT_TIME_NEEDS_DETAILS", emptyMap()),
        )

        val result = eteValidationService.validate(
          baselineCourseCompletionResolution.copy(
            creditTimeDetails = null,
          ),
          CourseCompletionValidationContext(baselineCourseCompletionEvent),
        )

        assertThat(result).hasErrors(expectedErrors)
        assertThat(result).hasNoWarnings()
      }

      @Test
      fun `error if invalid contact outcome code`() {
        every {
          contactOutcomeEntityRepository.findByCode(CONTACT_OUTCOME_CODE)
        } returns null
        val expectedErrors = listOf(
          ValidationResultItem(
            "$.creditTimeDetails.contactOutcomeCode",
            "UNKNOWN_CONTACT_OUTCOME",
            mapOf("code" to "CTC01"),
          ),
        )

        val result = eteValidationService.validate(
          baselineCourseCompletionResolution,
          CourseCompletionValidationContext(baselineCourseCompletionEvent),
        )

        assertThat(result).hasErrors(expectedErrors)
        assertThat(result).hasNoWarnings()
      }
    }

    @Nested
    inner class DontCreditTime {

      val baselineCourseCompletionResolution = CourseCompletionResolutionDto.valid().copy(
        type = CourseCompletionResolutionTypeDto.DONT_CREDIT_TIME,
        creditTimeDetails = null,
        dontCreditTimeDetails = CourseCompletionDontCreditTimeDetailsDto.valid(),
      )

      val baselineCourseCompletionEvent = EteCourseCompletionEventEntity.valid().copy(
        resolution = null,
      )

      @Test
      fun success() {
        val result = eteValidationService.validate(
          baselineCourseCompletionResolution,
          CourseCompletionValidationContext(baselineCourseCompletionEvent),
        )

        assertThat(result).hasNoErrors()
        assertThat(result).hasNoWarnings()
      }

      @Test
      fun `error if don't credit time details are not provided`() {
        val expectedErrors = listOf(
          ValidationResultItem("$.dontCreditTimeDetails", "DONT_CREDIT_TIME_NEEDS_DETAILS", emptyMap()),
        )

        val result = eteValidationService.validate(
          baselineCourseCompletionResolution.copy(
            dontCreditTimeDetails = null,
          ),
          CourseCompletionValidationContext(baselineCourseCompletionEvent),
        )

        assertThat(result).hasErrors(expectedErrors)
        assertThat(result).hasNoWarnings()
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
        val result = eteValidationService.validate(
          baselineCourseCompletionOutcome,
          CourseCompletionValidationContext(
            baselineCourseCompletionEvent.copy(
              resolution = null,
            ),
          ),
        )

        assertThat(result).hasNoErrors()
        assertThat(result).hasNoWarnings()
      }

      @Test
      fun `if existing resolution is logically identical, return EXISTING_IDENTICAL_RESOLUTION`() {
        val expectedWarnings = listOf(
          ValidationResultItem("$.creditTimeDetails", "EXISTING_IDENTICAL_RESOLUTION", emptyMap()),
        )

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

        val result = eteValidationService.validate(
          baselineCourseCompletionOutcome,
          CourseCompletionValidationContext(courseCompletionEvent),
        )

        assertThat(result).hasNoErrors()
        assertThat(result).hasWarnings(expectedWarnings)
      }

      @Test
      fun `if existing resolution is not logically identical, error`() {
        val courseCompletionEvent = baselineCourseCompletionEvent.copy(
          resolution = EteCourseCompletionEventResolutionEntity.valid(),
        )

        val expectedErrors = listOf(
          ValidationResultItem(
            "$.creditTimeDetails",
            "RESOLUTION_ALREADY_EXISTS",
            mapOf("id" to courseCompletionEvent.resolution!!.id),
          ),
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

        val result = eteValidationService.validate(
          baselineCourseCompletionOutcome,
          CourseCompletionValidationContext(courseCompletionEvent),
        )

        assertThat(result).hasErrors(expectedErrors)
        assertThat(result).hasNoWarnings()
      }
    }
  }
}
