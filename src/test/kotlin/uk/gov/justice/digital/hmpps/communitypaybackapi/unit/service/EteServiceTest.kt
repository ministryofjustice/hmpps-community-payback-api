package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntityRepository.ResolutionStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventResolutionRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseCompletionMessage
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.EteService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.EteValidationService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.ProjectService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.EteMappers
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto
import java.time.LocalDate
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
  lateinit var projectService: ProjectService

  @RelaxedMockK
  lateinit var eteMappers: EteMappers

  @RelaxedMockK
  lateinit var eteValidationService: EteValidationService

  @InjectMockKs
  private lateinit var eteService: EteService

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
  inner class GetEteCourseCompletionEvents {

    @Test
    fun `pass through to repository`() {
      val pageable = Pageable.unpaged()

      val pduId = UUID.randomUUID()
      val providerCode = "PC01"
      val offices = listOf("office1", "office2")
      val fromDate = LocalDate.of(2026, 1, 1)
      val toDate = LocalDate.of(2026, 12, 31)

      every {
        eteCourseCompletionEventEntityRepository.findAllWithFilters(
          providerCode = providerCode,
          pduId = pduId,
          officesCount = 2,
          offices = offices,
          resolutionStatus = ResolutionStatus.ANY,
          fromDate,
          toDate,
          pageable,
        )
      } returns PageImpl(
        listOf(
          EteCourseCompletionEventEntity.valid().copy(
            completionDate = LocalDate.of(2026, 6, 15),
          ),
        ),
      )

      val result = eteService.getCourseCompletionEvents(
        providerCode = providerCode,
        pduId = pduId,
        offices = offices,
        resolutionStatus = null,
        fromDate = fromDate,
        toDate = toDate,
        pageable = pageable,
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
      val entity = EteCourseCompletionEventEntity.valid()

      every { eteCourseCompletionEventEntityRepository.findByIdOrNull(eventId) } returns entity

      val result = eteService.getCourseCompletionEvent(eventId)

      assertThat(result).isEqualTo(entity.toDto())
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
}
