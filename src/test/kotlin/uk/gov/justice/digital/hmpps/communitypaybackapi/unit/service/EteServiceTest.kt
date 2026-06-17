package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.atFirstSecondOfDay
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.atLastSecondOfDay
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.EteCourseCompletionShowCourseFailuresDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntityRepository.CourseFailureFilter
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntityRepository.ResolutionStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventResolutionRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.listener.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseCompletionMessage
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.ContextService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.EteService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.EteValidationService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.SpringEventPublisher
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.EteMappers
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@ExtendWith(MockKExtension::class)
class EteServiceTest {

  @RelaxedMockK
  lateinit var eteCourseCompletionEventEntityRepository: EteCourseCompletionEventEntityRepository

  @RelaxedMockK
  lateinit var eteCourseCompletionEventResolutionRepository: EteCourseCompletionEventResolutionRepository

  @RelaxedMockK
  lateinit var appointmentService: AppointmentService

  @RelaxedMockK
  lateinit var eteMappers: EteMappers

  @RelaxedMockK
  lateinit var eteValidationService: EteValidationService

  @RelaxedMockK
  lateinit var contextService: ContextService

  @RelaxedMockK
  lateinit var springEventPublisher: SpringEventPublisher

  private lateinit var eteService: EteService

  val londonDateFrom: OffsetDateTime = OffsetDateTime.parse("2026-05-26T00:00:00Z")
  val londonDateTo: OffsetDateTime = OffsetDateTime.parse("2026-05-26T23:59:59.999999Z")
  val southCentralDateFrom: OffsetDateTime = OffsetDateTime.parse("2026-05-26T00:00:00Z")
  val southCentralTo: OffsetDateTime = OffsetDateTime.parse("2026-05-26T23:59:59.999999Z")
  val defaultDateFrom: OffsetDateTime = OffsetDateTime.parse("2026-05-20T00:00:00Z")
  val defaultDateTo: OffsetDateTime = OffsetDateTime.parse("2026-05-20T23:59:59.999999Z")

  @BeforeEach
  fun setUp() {
    eteService = EteService(
      eteMappers,
      eteCourseCompletionEventEntityRepository,
      eteCourseCompletionEventResolutionRepository,
      appointmentService,
      eteValidationService,
      contextService,
      springEventPublisher,
      defaultDateFrom,
      defaultDateTo,
      londonDateFrom,
      londonDateTo,
      southCentralDateFrom,
      southCentralTo,
    )
  }

  @Nested
  inner class HandleEducationCourseMessage {

    @Test
    fun `create ete course event entry with completed status`() {
      val message = EducationCourseCompletionMessage.valid()

      val mappingResult = EteCourseCompletionEventEntity.valid()
      every { eteMappers.toCourseCompletionEventEntity(message) } returns mappingResult
      every { eteCourseCompletionEventEntityRepository.save(any()) } returnsArgument 0

      eteService.recordCourseCompletionEvent(message)

      verify { eteCourseCompletionEventEntityRepository.save(mappingResult) }
    }
  }

  @Nested
  inner class GetPassedEteCourseCompletionEvents {

    @Test
    fun `pass through to repository`() {
      val pageable = Pageable.unpaged()

      val pduId = UUID.randomUUID()
      val providerCode = "PC01"
      val offices = listOf("office1", "office2")
      val attempts = 1
      val externalReference = "EXT-REF-123456"
      val fromDate = LocalDate.of(2026, 1, 1).atFirstSecondOfDay()
      val toDate = LocalDate.of(2026, 12, 31).atLastSecondOfDay()

      eteService = EteService(
        eteMappers,
        eteCourseCompletionEventEntityRepository,
        eteCourseCompletionEventResolutionRepository,
        appointmentService,
        eteValidationService,
        contextService,
        springEventPublisher,
        null,
        null,
        null,
        null,
        null,
        null,
      )

      every {
        eteCourseCompletionEventEntityRepository.findAllWithFilters(
          providerCode = providerCode,
          pduId = pduId,
          officesCount = 2,
          offices = offices,
          resolutionStatus = ResolutionStatus.ANY,
          courseFailures = CourseFailureFilter.SHOW_ONLY_WHEN_MAX_ATTEMPTS_REACHED,
          externalReference = externalReference,
          fromDate = fromDate,
          toDate = toDate,
          availableFromDate = any(),
          availableToDate = any(),
          pageable = pageable,
        )
      } returns PageImpl(
        listOf(
          EteCourseCompletionEventEntity.valid().copy(
            completionDateTime = LocalDate.of(2026, 6, 15).atFirstSecondOfDay(),
          ),
        ),
      )

      val result = eteService.getCourseCompletionEvents(
        providerCode = providerCode,
        pduId = pduId,
        offices = offices,
        resolutionStatus = null,
        showCourseFailures = EteCourseCompletionShowCourseFailuresDto.OnlyWhenMaxAttemptsReached,
        externalReference = externalReference,
        fromDate = fromDate,
        toDate = toDate,
        pageable = pageable,
      )

      assertThat(result.isEmpty).isFalse
      assertThat(result.content).hasSize(1)
      assertThat(result.content[0].completionDateTime).isEqualTo(LocalDate.of(2026, 6, 15).atFirstSecondOfDay())

      verify {
        eteCourseCompletionEventEntityRepository.findAllWithFilters(
          providerCode = providerCode,
          pduId = pduId,
          officesCount = 2,
          offices = offices,
          resolutionStatus = ResolutionStatus.ANY,
          courseFailures = CourseFailureFilter.SHOW_ONLY_WHEN_MAX_ATTEMPTS_REACHED,
          externalReference = externalReference,
          fromDate = fromDate,
          toDate = toDate,
          availableFromDate = null,
          availableToDate = null,
          pageable = pageable,
        )
      }
    }

    @Test
    fun `use correct available dates when configured for London`() {
      val pageable = Pageable.unpaged()
      val providerCode = "N07"

      every {
        eteCourseCompletionEventEntityRepository.findAllWithFilters(
          any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
        )
      } returns PageImpl(emptyList())

      eteService.getCourseCompletionEvents(
        providerCode = providerCode,
        pduId = null,
        offices = null,
        resolutionStatus = null,
        showCourseFailures = EteCourseCompletionShowCourseFailuresDto.OnlyWhenMaxAttemptsReached,
        externalReference = null,
        fromDate = null,
        toDate = null,
        pageable = pageable,
      )

      verify {
        eteCourseCompletionEventEntityRepository.findAllWithFilters(
          providerCode = providerCode,
          pduId = null,
          officesCount = 0,
          offices = emptyList(),
          resolutionStatus = ResolutionStatus.ANY,
          courseFailures = CourseFailureFilter.SHOW_ONLY_WHEN_MAX_ATTEMPTS_REACHED,
          externalReference = null,
          fromDate = null,
          toDate = null,
          availableFromDate = londonDateFrom,
          availableToDate = londonDateTo,
          pageable = pageable,
        )
      }
    }

    @Test
    fun `use correct availableFromDate when configured for South Central`() {
      val pageable = Pageable.unpaged()
      val providerCode = "N59"

      every {
        eteCourseCompletionEventEntityRepository.findAllWithFilters(
          any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
        )
      } returns PageImpl(emptyList())

      eteService.getCourseCompletionEvents(
        providerCode = providerCode,
        pduId = null,
        offices = null,
        resolutionStatus = null,
        showCourseFailures = EteCourseCompletionShowCourseFailuresDto.OnlyWhenMaxAttemptsReached,
        externalReference = null,
        fromDate = null,
        toDate = null,
        pageable = pageable,
      )

      verify {
        eteCourseCompletionEventEntityRepository.findAllWithFilters(
          providerCode = providerCode,
          pduId = null,
          officesCount = 0,
          offices = emptyList(),
          resolutionStatus = ResolutionStatus.ANY,
          courseFailures = CourseFailureFilter.SHOW_ONLY_WHEN_MAX_ATTEMPTS_REACHED,
          externalReference = null,
          fromDate = null,
          toDate = null,
          availableFromDate = southCentralDateFrom,
          availableToDate = southCentralTo,
          pageable = pageable,
        )
      }
    }

    @Test
    fun `use default availableFromDate for other regions even on production`() {
      val pageable = Pageable.unpaged()
      val providerCode = "OTHER"
      val defaultDateFrom = OffsetDateTime.parse("2026-05-20T00:00:00Z")
      val defaultDateTo = OffsetDateTime.parse("2026-05-20T23:59:59.999999Z")

      every {
        eteCourseCompletionEventEntityRepository.findAllWithFilters(
          any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
        )
      } returns PageImpl(emptyList())

      eteService.getCourseCompletionEvents(
        providerCode = providerCode,
        pduId = null,
        offices = null,
        resolutionStatus = null,
        showCourseFailures = EteCourseCompletionShowCourseFailuresDto.OnlyWhenMaxAttemptsReached,
        externalReference = null,
        fromDate = null,
        toDate = null,
        pageable = pageable,
      )

      verify {
        eteCourseCompletionEventEntityRepository.findAllWithFilters(
          providerCode = providerCode,
          pduId = null,
          officesCount = 0,
          offices = emptyList(),
          resolutionStatus = ResolutionStatus.ANY,
          courseFailures = CourseFailureFilter.SHOW_ONLY_WHEN_MAX_ATTEMPTS_REACHED,
          externalReference = null,
          fromDate = null,
          toDate = null,
          availableFromDate = defaultDateFrom,
          availableToDate = defaultDateTo,
          pageable = pageable,
        )
      }
    }
  }

  @Nested
  inner class GetCourseCompletionEvent {

    @Test
    fun `should return course completion event when found`() {
      val eventId = UUID.randomUUID()
      val entity = EteCourseCompletionEventEntity.valid()

      every { eteCourseCompletionEventEntityRepository.findByIdOrNull(eventId) } returns entity

      val result = eteService.getCourseCompletionEvent(eventId)

      assertThat(result).isEqualTo(entity.toDto())
    }

    @Test
    fun `returns null when event not found`() {
      val eventId = UUID.randomUUID()

      every { eteCourseCompletionEventEntityRepository.findByIdOrNull(eventId) } returns null

      val result = eteService.getCourseCompletionEvent(eventId)
      assertThat(result).isNull()
    }
  }

  @Nested
  inner class GetCourseCompletionBlock {

    @Test
    fun `should return correct block for given id and block size`() {
      val externalReference = "EXT-REF-1"
      val id100 = UUID.randomUUID()
      val id101 = UUID.randomUUID()
      val id102 = UUID.randomUUID()
      val id125 = UUID.randomUUID()

      val e100 = EteCourseCompletionEventEntity.valid().copy(id = id100, externalReference = externalReference, attempts = 1)
      val e101 = EteCourseCompletionEventEntity.valid().copy(id = id101, externalReference = externalReference, attempts = 2)
      val e102 = EteCourseCompletionEventEntity.valid().copy(id = id102, externalReference = externalReference, attempts = 3)
      val e125 = EteCourseCompletionEventEntity.valid().copy(id = id125, externalReference = externalReference, attempts = 4)

      val allEvents = listOf(e100, e101, e102, e125)

      every { eteCourseCompletionEventEntityRepository.findByIdOrNull(id102) } returns e102
      every {
        eteCourseCompletionEventEntityRepository.findBlock(
          providerCode = e102.pdu.providerCode,
          externalReference = externalReference,
          startAttempt = 1,
          endAttempt = 3,
        )
      } returns listOf(e100, e101, e102)

      val result = eteService.getCourseCompletionBlock(id102, 3)

      assertThat(result.map { it.id }).containsExactly(id100, id101, id102)
    }

    @Test
    fun `should return correct block when an attempt is missing`() {
      val externalReference = "EXT-REF-1"
      val id101 = UUID.randomUUID()
      val id102 = UUID.randomUUID()
      val id103 = UUID.randomUUID()

      val e101 = EteCourseCompletionEventEntity.valid().copy(id = id101, externalReference = externalReference, attempts = 2)
      val e102 = EteCourseCompletionEventEntity.valid().copy(id = id102, externalReference = externalReference, attempts = 3)
      val e103 = EteCourseCompletionEventEntity.valid().copy(id = id103, externalReference = externalReference, attempts = 4)

      // Attempt 1 is missing.
      // Block for attempts 1-3 should contain only e101 and e102.

      every { eteCourseCompletionEventEntityRepository.findByIdOrNull(id101) } returns e101

      every {
        eteCourseCompletionEventEntityRepository.findBlock(
          providerCode = e101.pdu.providerCode,
          externalReference = externalReference,
          startAttempt = 1,
          endAttempt = 3,
        )
      } returns listOf(e101, e102)

      val result = eteService.getCourseCompletionBlock(id101, 3)

      // Currently, it will return [e101, e102, e103] because Page 0 (size 3) contains all 3.
      // But it should return [e101, e102].
      assertThat(result.map { it.id }).containsExactly(id101, id102)
    }

    @Test
    fun `should return correct block for last element in the list`() {
      val externalReference = "EXT-REF-1"
      val id100 = UUID.randomUUID()
      val id101 = UUID.randomUUID()
      val id102 = UUID.randomUUID()
      val id125 = UUID.randomUUID()

      val e100 = EteCourseCompletionEventEntity.valid().copy(id = id100, externalReference = externalReference, attempts = 1)
      val e101 = EteCourseCompletionEventEntity.valid().copy(id = id101, externalReference = externalReference, attempts = 2)
      val e102 = EteCourseCompletionEventEntity.valid().copy(id = id102, externalReference = externalReference, attempts = 3)
      val e125 = EteCourseCompletionEventEntity.valid().copy(id = id125, externalReference = externalReference, attempts = 4)

      val allEvents = listOf(e100, e101, e102, e125)

      every { eteCourseCompletionEventEntityRepository.findByIdOrNull(id125) } returns e125
      every {
        eteCourseCompletionEventEntityRepository.findBlock(
          providerCode = e125.pdu.providerCode,
          externalReference = externalReference,
          startAttempt = 4,
          endAttempt = 6,
        )
      } returns listOf(e125)

      val result = eteService.getCourseCompletionBlock(id125, 3)

      assertThat(result.map { it.id }).containsExactly(id125)
    }

    @Test
    fun `should return empty list if event id not found in list of events for external reference`() {
      val externalReference = "EXT-REF-1"
      val id = UUID.randomUUID()
      val event = EteCourseCompletionEventEntity.valid().copy(id = id, externalReference = externalReference)

      every { eteCourseCompletionEventEntityRepository.findByIdOrNull(id) } returns event
      every {
        eteCourseCompletionEventEntityRepository.findBlock(
          providerCode = event.pdu.providerCode,
          externalReference = externalReference,
          startAttempt = 1,
          endAttempt = 3,
        )
      } returns listOf(event)

      val result = eteService.getCourseCompletionBlock(id, 3)

      assertThat(result.map { it.id }).containsExactly(id)
    }

    @Test
    fun `should return correct block even if attempts is null`() {
      val externalReference = "EXT-REF-1"
      val id = UUID.randomUUID()
      val event = EteCourseCompletionEventEntity.valid().copy(id = id, externalReference = externalReference, attempts = null)

      every { eteCourseCompletionEventEntityRepository.findByIdOrNull(id) } returns event
      every {
        eteCourseCompletionEventEntityRepository.findBlock(
          providerCode = event.pdu.providerCode,
          externalReference = externalReference,
          startAttempt = 1,
          endAttempt = 3,
        )
      } returns listOf(event.copy(attempts = 1)) // Simulate it being found or treated as 1

      val result = eteService.getCourseCompletionBlock(id, 3)

      assertThat(result).isNotEmpty
    }

    @Test
    fun `should throw exception if event not found in repository`() {
      val id = UUID.randomUUID()
      every { eteCourseCompletionEventEntityRepository.findByIdOrNull(id) } returns null

      val exception = org.junit.jupiter.api.assertThrows<IllegalStateException> {
        eteService.getCourseCompletionBlock(id, 3)
      }
      assertThat(exception.message).isEqualTo("Can't find course completion event $id")
    }

    @Test
    fun `should throw exception if block size is less than 1`() {
      val id = UUID.randomUUID()
      val exception = org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
        eteService.getCourseCompletionBlock(id, 0)
      }
      assertThat(exception.message).isEqualTo("blockSize must be greater than 0")
    }
  }
}
