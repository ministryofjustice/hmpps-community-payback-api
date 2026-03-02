package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.EteValidationService

@ExtendWith(MockKExtension::class)
class EteValidationServiceTest {

  @RelaxedMockK
  lateinit var contactOutcomeEntityRepository: ContactOutcomeEntityRepository

  @InjectMockKs
  private lateinit var eteValidationService: EteValidationService

  @Nested
  inner class ValidateCourseCompletionOutcome {

    @Test
    fun success() {
      every {
        contactOutcomeEntityRepository.findByCode("CTC01")
      } returns ContactOutcomeEntity.valid()

      eteValidationService.validateCourseCompletionOutcome(
        CourseCompletionOutcomeDto.valid().copy(
          contactOutcomeCode = "CTC01",
        ),
      )
    }
  }
}
