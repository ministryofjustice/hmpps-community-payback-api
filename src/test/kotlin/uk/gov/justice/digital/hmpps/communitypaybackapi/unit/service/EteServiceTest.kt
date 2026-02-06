package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseEventStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalDate
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseCompletionMessage
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseCompletionStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseMessageAttributes
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentCreationService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.EteService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.EducationCourseCompletionMapper
import java.time.LocalDate
import java.util.Optional.empty
import java.util.UUID

@ExtendWith(MockKExtension::class)
class EteServiceTest {

  @RelaxedMockK
  lateinit var eteCourseCompletionEventEntityRepository: EteCourseCompletionEventEntityRepository

  @RelaxedMockK
  lateinit var appointmentCreationService: AppointmentCreationService

  @RelaxedMockK
  lateinit var educationCourseCompletionMapper: EducationCourseCompletionMapper

  @InjectMockKs
  private lateinit var service: EteService

  @Nested
  inner class HandleEducationCourseMessage {

    @Test
    fun `create ete course event entry`() {
      val entityCaptor = slot<EteCourseCompletionEventEntity>()
      every { eteCourseCompletionEventEntityRepository.save(capture(entityCaptor)) } returnsArgument 0

      service.handleEducationCourseCompletionMessage(
        EducationCourseCompletionMessage.valid().copy(
          messageAttributes = EducationCourseMessageAttributes.valid().copy(
            externalReference = "EXT123",
            firstName = "John",
            lastName = "Doe",
            dateOfBirth = LocalDate.of(1990, 5, 15),
            region = "London",
            email = "john.doe@example.com",
            courseName = "The course name",
            courseType = "Online",
            provider = "Training Provider Inc.",
            totalTimeMinutes = 70,
            expectedTimeMinutes = 120,
            status = EducationCourseCompletionStatus.Failed,
            completionDate = LocalDate.of(2026, 1, 1),
          ),
        ),
      )

      assertThat(entityCaptor.isCaptured).isTrue
      val persistedEntity = entityCaptor.captured

      // Person data assertions
      assertThat(persistedEntity.firstName).isEqualTo("John")
      assertThat(persistedEntity.lastName).isEqualTo("Doe")
      assertThat(persistedEntity.dateOfBirth).isEqualTo(LocalDate.of(1990, 5, 15))
      assertThat(persistedEntity.region).isEqualTo("London")
      assertThat(persistedEntity.email).isEqualTo("john.doe@example.com")

      // Course data assertions
      assertThat(persistedEntity.courseName).isEqualTo("The course name")
      assertThat(persistedEntity.courseType).isEqualTo("Online")
      assertThat(persistedEntity.provider).isEqualTo("Training Provider Inc.")
      assertThat(persistedEntity.totalTimeMinutes).isEqualTo(70)
      assertThat(persistedEntity.expectedTimeMinutes).isEqualTo(120)
      assertThat(persistedEntity.status).isEqualTo(EteCourseEventStatus.FAILED)
      assertThat(persistedEntity.completionDate).isEqualTo("2026-01-01")

      // External ID assertion
      assertThat(persistedEntity.externalReference).isEqualTo("EXT123")

      // Verify UUID is generated
      assertThat(persistedEntity.id).isNotNull
    }

    @Test
    fun `create ete course event entry with completed status`() {
      val entityCaptor = slot<EteCourseCompletionEventEntity>()
      every { eteCourseCompletionEventEntityRepository.save(capture(entityCaptor)) } returnsArgument 0

      service.handleEducationCourseCompletionMessage(
        EducationCourseCompletionMessage.valid().copy(
          messageAttributes = EducationCourseMessageAttributes.valid().copy(
            externalReference = "EXT456",
            firstName = "Jane",
            lastName = "Smith",
            dateOfBirth = LocalDate.of(1985, 8, 22),
            region = "Manchester",
            email = "jane.smith@example.com",
            courseName = "Advanced Course",
            courseType = "In-person",
            provider = "Education Corp",
            totalTimeMinutes = 150,
            expectedTimeMinutes = 150,
            status = EducationCourseCompletionStatus.Completed,
            completionDate = randomLocalDate(),
          ),
        ),
      )

      assertThat(entityCaptor.isCaptured).isTrue
      val persistedEntity = entityCaptor.captured

      assertThat(persistedEntity.status).isEqualTo(EteCourseEventStatus.COMPLETED)
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
      val result = service.getEteCourseCompletionEvents("INVALID", null, null, pageable)

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
    fun `should return course completion events filtered by date range`(providerCode: String, region: String) {
      val pageable = Pageable.unpaged()
      val fromDate = LocalDate.of(2026, 1, 1)
      val toDate = LocalDate.of(2026, 12, 31)
      val entity = EteCourseCompletionEventEntity(
        id = UUID.randomUUID(),
        firstName = "Jane",
        lastName = "Smith",
        dateOfBirth = LocalDate.of(1985, 8, 22),
        region = region,
        email = "jane.smith@example.com",
        courseName = "Advanced Course",
        courseType = "Course Type",
        provider = "Moodle",
        completionDate = LocalDate.of(2026, 6, 15),
        status = EteCourseEventStatus.COMPLETED,
        totalTimeMinutes = 150,
        expectedTimeMinutes = 150,
        externalReference = "EXT456",
        attempts = 1,
      )

      every {
        eteCourseCompletionEventEntityRepository.findByRegionAndDateRange(
          region,
          fromDate,
          toDate,
          pageable,
        )
      } returns PageImpl(listOf(entity))

      val result = service.getEteCourseCompletionEvents(providerCode, fromDate, toDate, pageable)

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
        email = "john.doe@example.com",
        courseName = "Test Course",
        courseType = "Online",
        provider = "Test Provider",
        completionDate = LocalDate.of(2026, 1, 1),
        status = EteCourseEventStatus.COMPLETED,
        totalTimeMinutes = 120,
        expectedTimeMinutes = 120,
        externalReference = "EXT123",
        attempts = 1,
      )

      every { eteCourseCompletionEventEntityRepository.findById(eventId) } returns java.util.Optional.of(entity)

      val result = service.getCourseCompletionEvent(eventId)

      assertThat(result.id).isEqualTo(eventId)
      assertThat(result.firstName).isEqualTo("John")
      assertThat(result.lastName).isEqualTo("Doe")
      assertThat(result.courseName).isEqualTo("Test Course")
      assertThat(result.status).isEqualTo(EteCourseEventStatus.COMPLETED)
    }

    @Test
    fun `throws NotFoundException when event not found`() {
      val eventId = UUID.randomUUID()

      every { eteCourseCompletionEventEntityRepository.findById(eventId) } returns empty()

      assertThrows<NotFoundException> {
        service.getCourseCompletionEvent(eventId)
      }.also {
        assertThat(it.message).contains("Course completion event")
        assertThat(it.message).contains(eventId.toString())
      }
    }
  }
}
