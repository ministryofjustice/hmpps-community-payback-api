package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntityRepository.ResolutionStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventResolutionEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventResolutionRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseCompletionMessage
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseCompletionStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseMessageAttributes
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.EteService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.EteValidationService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.EteValidationService.ValidationResult
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.ProjectService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.EteMappers
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockKExtension::class)
class EteServiceTest {

  @RelaxedMockK
  lateinit var eteCourseCompletionEventEntityRepository: EteCourseCompletionEventEntityRepository

  @RelaxedMockK
  lateinit var eteCourseCompletionEventResolutionRepository: EteCourseCompletionEventResolutionRepository

  @RelaxedMockK
  lateinit var contactOutcomeEntityRepository: ContactOutcomeEntityRepository

  @RelaxedMockK
  lateinit var appointmentService: AppointmentService

  @RelaxedMockK
  lateinit var projectService: ProjectService

  @RelaxedMockK
  lateinit var eteMappers: EteMappers

  @RelaxedMockK
  lateinit var eteValidationService: EteValidationService

  @RelaxedMockK
  lateinit var communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient

  @InjectMockKs
  private lateinit var eteService: EteService

  companion object {
    const val CONTACT_OUTCOME_CODE = "CTC01"
  }

  @Nested
  inner class HandleEducationCourseMessage {

    @Test
    fun `create ete course event entry with completed status`() {
      val entityCaptor = slot<EteCourseCompletionEventEntity>()
      every { eteCourseCompletionEventEntityRepository.save(capture(entityCaptor)) } returnsArgument 0

      eteService.recordCourseCompletionEvent(
        EducationCourseCompletionMessage.valid().copy(
          messageAttributes = EducationCourseMessageAttributes.valid().copy(
            externalReference = "EXT456",
            totalTimeMinutes = 150,
            expectedTimeMinutes = 150,
            status = EducationCourseCompletionStatus.Completed,
          ),
        ),
      )

      assertThat(entityCaptor.isCaptured).isTrue
      val persistedEntity = entityCaptor.captured

      assertThat(persistedEntity.status).isEqualTo(EteCourseCompletionEventStatus.COMPLETED)
      assertThat(persistedEntity.totalTimeMinutes).isEqualTo(150) // 2 hours 30 minutes = 150 minutes
      assertThat(persistedEntity.expectedTimeMinutes).isEqualTo(150)
      assertThat(persistedEntity.externalReference).isEqualTo("EXT456")

      // ensure appointment is created
    }
  }

  @Nested
  inner class GetEteCourseCompletionEvents {

    @Test
    fun `should return empty page when provider code not found`() {
      val pageable = Pageable.unpaged()
      val result = eteService.getCourseCompletionEvents("INVALID", null, null, null, null, pageable)

      assertThat(result.isEmpty).isTrue
    }

    @ParameterizedTest
    @CsvSource(
      "N07, London",
      "N56, East of England",
      "N53, East Midlands",
      "N52, West Midlands",
      "N50, Greater Manchester",
      "N57, 'Kent, Surrey and Sussex'",
      "N54, North East",
      "N51, North West",
      "N59, South Central",
      "N58, South West",
      "N03, Wales",
      "N55, Yorks & Humber",
    )
    fun `use correct region code mapping`(providerCode: String, region: String) {
      val pageable = Pageable.unpaged()
      val offices = listOf("office1", "office2")
      val fromDate = LocalDate.of(2026, 1, 1)
      val toDate = LocalDate.of(2026, 12, 31)
      val entity = EteCourseCompletionEventEntity.valid().copy(
        region = region,
        completionDate = LocalDate.of(2026, 6, 15),
      )

      every {
        eteCourseCompletionEventEntityRepository.findAllWithFilters(
          region,
          officesCount = 2,
          offices = offices,
          resolutionStatus = ResolutionStatus.ANY,
          fromDate,
          toDate,
          pageable,
        )
      } returns PageImpl(listOf(entity))

      val result = eteService.getCourseCompletionEvents(
        providerCode,
        fromDate,
        toDate,
        offices,
        resolutionStatus = null,
        pageable,
      )

      assertThat(result.isEmpty).isFalse
      assertThat(result.content).hasSize(1)
      assertThat(result.content[0].completionDate).isEqualTo("2026-06-15")
    }
  }

  @Nested
  inner class GetCourseCompletionEvent {

    @Test
    fun `should return course completion event when found`() {
      val eventId = UUID.randomUUID()
      val entity = EteCourseCompletionEventEntity(
        id = eventId,
        firstName = "John",
        lastName = "Doe",
        dateOfBirth = LocalDate.of(1990, 5, 15),
        region = "London",
        office = "Office 123",
        email = "john.doe@example.com",
        courseName = "Test Course",
        courseType = "Online",
        provider = "Test Provider",
        completionDate = LocalDate.of(2026, 1, 1),
        status = EteCourseCompletionEventStatus.COMPLETED,
        totalTimeMinutes = 120,
        expectedTimeMinutes = 120,
        externalReference = "EXT123",
        attempts = 1,
      )

      every { eteCourseCompletionEventEntityRepository.findByIdOrNull(eventId) } returns entity

      val result = eteService.getCourseCompletionEvent(eventId)

      assertThat(result.id).isEqualTo(eventId)
      assertThat(result.firstName).isEqualTo("John")
      assertThat(result.lastName).isEqualTo("Doe")
      assertThat(result.courseName).isEqualTo("Test Course")
      assertThat(result.status).isEqualTo(EteCourseCompletionEventStatus.COMPLETED)
    }

    @Test
    fun `throws NotFoundException when event not found`() {
      val eventId = UUID.randomUUID()

      every { eteCourseCompletionEventEntityRepository.findByIdOrNull(eventId) } returns null

      assertThrows<NotFoundException> {
        eteService.getCourseCompletionEvent(eventId)
      }.also {
        assertThat(it.message).contains("Course completion event")
        assertThat(it.message).contains(eventId.toString())
      }
    }
  }

  @Nested
  inner class ValidateCourseCompletionOutcome {

    val baselineCourseCompletionOutcome = CourseCompletionOutcomeDto.valid().copy(
      contactOutcomeCode = CONTACT_OUTCOME_CODE,
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

      @BeforeEach
      fun setup() {
        eteValidationService = EteValidationService(
          contactOutcomeEntityRepository = contactOutcomeEntityRepository,
          eteMapper = eteMappers,
        )
      }

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
      @BeforeEach
      fun setup() {
        eteValidationService = EteValidationService(
          contactOutcomeEntityRepository = contactOutcomeEntityRepository,
          eteMapper = eteMappers,
        )
      }

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
