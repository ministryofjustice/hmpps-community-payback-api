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
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionOutcomeDto
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

  val baselineCourseCompletionOutcome = CourseCompletionOutcomeDto.valid().copy(
    contactOutcomeCode = CONTACT_OUTCOME_CODE,
  )

  val baselineCourseCompletionEvent = EteCourseCompletionEventEntity.valid().copy(
    resolution = null,
  )

  @Nested
  inner class ValidateCourseCompletionOutcome {

    @BeforeEach
    fun baselineMocks() {
      every {
        contactOutcomeEntityRepository.findByCode(CONTACT_OUTCOME_CODE)
      } returns ContactOutcomeEntity.valid()
    }

    @Nested
    inner class Success {

      @Test
      fun success() {
        eteValidationService.validateCourseCompletionOutcome(
          baselineCourseCompletionOutcome,
          baselineCourseCompletionEvent,
        )
      }
    }

    @Nested
    inner class ContactOutcome {

      @Test
      fun `error if invalid contact outcome code`() {
        every {
          contactOutcomeEntityRepository.findByCode(CONTACT_OUTCOME_CODE)
        } returns null

        assertThatThrownBy {
          eteValidationService.validateCourseCompletionOutcome(
            baselineCourseCompletionOutcome,
            baselineCourseCompletionEvent,
          )
        }.hasMessage("Cannot find contact outcome with code CTC01")
      }
    }

    @Nested
    inner class ExistingResolution {

      @Test
      fun `if no existing resolution, is valid`() {
        eteValidationService.validateCourseCompletionOutcome(
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
          eteMappers.toResolutionEntity(
            id = any(),
            courseCompletionEvent = courseCompletionEvent,
            courseCompletionOutcome = baselineCourseCompletionOutcome,
            deliusAppointmentId = baselineCourseCompletionOutcome.appointmentIdToUpdate!!,
          )
        } returns courseCompletionEvent.resolution!!.copy()

        val result = eteValidationService.validateCourseCompletionOutcome(
          outcome = baselineCourseCompletionOutcome,
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
          eteMappers.toResolutionEntity(
            id = any(),
            courseCompletionEvent = courseCompletionEvent,
            courseCompletionOutcome = baselineCourseCompletionOutcome,
            deliusAppointmentId = baselineCourseCompletionOutcome.appointmentIdToUpdate!!,
          )
        } returns courseCompletionEvent.resolution!!.copy(
          projectCode = "some other project code",
        )

        assertThatThrownBy {
          eteValidationService.validateCourseCompletionOutcome(
            outcome = baselineCourseCompletionOutcome,
            courseCompletionEvent = courseCompletionEvent,
          )
        }.hasMessage("A resolution has already been defined for this course completion record")
      }
    }
  }
}
