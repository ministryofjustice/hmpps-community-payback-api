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
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseCompletionMessage
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseCompletionStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseMessageAttributes
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentEventTrigger
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.ContextService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.EteService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.EteValidationService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.EteMappers
import java.time.LocalDate
import java.util.Optional.empty
import java.util.UUID
import kotlin.random.Random

@ExtendWith(MockKExtension::class)
class EteServiceTest {

  @RelaxedMockK
  lateinit var eteCourseCompletionEventEntityRepository: EteCourseCompletionEventEntityRepository

  @RelaxedMockK
  lateinit var appointmentService: AppointmentService

  @RelaxedMockK
  lateinit var educationCourseCompletionMapper: EteMappers

  @RelaxedMockK
  lateinit var contextService: ContextService

  @RelaxedMockK
  lateinit var eteValidationService: EteValidationService

  @InjectMockKs
  private lateinit var eteService: EteService

  @Nested
  inner class HandleEducationCourseMessage {

    @Test
    fun `create ete course event entry`() {
      val entityCaptor = slot<EteCourseCompletionEventEntity>()
      every { eteCourseCompletionEventEntityRepository.save(capture(entityCaptor)) } returnsArgument 0

      val entity = EducationCourseCompletionMessage.valid().copy(
        messageAttributes = EducationCourseMessageAttributes.valid().copy(
          externalReference = "EXT123",
          firstName = "John",
          lastName = "Doe",
          dateOfBirth = LocalDate.of(1990, 5, 15),
          region = "London",
          office = "The Office",
          email = "john.doe@example.com",
          courseName = "The course name",
          courseType = "Online",
          provider = "Training Provider Inc.",
          totalTimeMinutes = 70,
          expectedTimeMinutes = 120,
          status = EducationCourseCompletionStatus.Failed,
          completionDate = LocalDate.of(2026, 1, 1),
        ),
      )

      eteService.handleEducationCourseCompletionMessage(entity)

      assertThat(entityCaptor.isCaptured).isTrue
      val persistedEntity = entityCaptor.captured

      // Person data assertions
      assertThat(persistedEntity.firstName).isEqualTo("John")
      assertThat(persistedEntity.lastName).isEqualTo("Doe")
      assertThat(persistedEntity.dateOfBirth).isEqualTo(LocalDate.of(1990, 5, 15))
      assertThat(persistedEntity.region).isEqualTo("London")
      assertThat(persistedEntity.office).isEqualTo("The Office")
      assertThat(persistedEntity.email).isEqualTo("john.doe@example.com")

      // Course data assertions
      assertThat(persistedEntity.courseName).isEqualTo("The course name")
      assertThat(persistedEntity.courseType).isEqualTo("Online")
      assertThat(persistedEntity.provider).isEqualTo("Training Provider Inc.")
      assertThat(persistedEntity.totalTimeMinutes).isEqualTo(70)
      assertThat(persistedEntity.expectedTimeMinutes).isEqualTo(120)
      assertThat(persistedEntity.status).isEqualTo(EteCourseCompletionEventStatus.FAILED)
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

      eteService.handleEducationCourseCompletionMessage(
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
      val result = eteService.getEteCourseCompletionEvents("INVALID", null, null, null, pageable)

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
      val entity = EteCourseCompletionEventEntity.valid().copy(
        region = region,
        completionDate = LocalDate.of(2026, 6, 15),
      )

      every {
        eteCourseCompletionEventEntityRepository.findByRegionAndDateRange(
          region,
          fromDate,
          toDate,
          pageable,
        )
      } returns PageImpl(listOf(entity))

      val result = eteService.getEteCourseCompletionEvents(providerCode, fromDate, toDate, null, pageable)

      assertThat(result.isEmpty).isFalse
      assertThat(result.content).hasSize(1)
      assertThat(result.content[0].completionDate).isEqualTo("2026-06-15")
    }

    fun `should return course completion events filtered by office`() {
      val pageable = Pageable.unpaged()
      val providerCode = "N07"
      val office = "The Office"
      val startDate = LocalDate.of(2026, 1, 1)
      val endDate = LocalDate.of(2026, 12, 31)

      (1..5).forEach {
        eteCourseCompletionEventEntityRepository.save(
          EteCourseCompletionEventEntity.valid().copy(
            office = office,
            provider = providerCode,
            completionDate = endDate.minusDays(Random.nextLong(21)),
          ),
        )
      }

      (1..3).forEach {
        eteCourseCompletionEventEntityRepository.save(
          EteCourseCompletionEventEntity.valid().copy(
            office = String.random(21),
            provider = providerCode,
            completionDate = endDate.minusDays(Random.nextLong(21)),
          ),
        )
      }

      val result = eteService.getEteCourseCompletionEvents(
        providerCode,
        startDate,
        endDate,
        null,
        pageable,
      )

      assertThat(result.isEmpty).isFalse
      assertThat(result.content).hasSize(5)
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

      every { eteCourseCompletionEventEntityRepository.findById(eventId) } returns java.util.Optional.of(entity)

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

      every { eteCourseCompletionEventEntityRepository.findById(eventId) } returns empty()

      assertThrows<NotFoundException> {
        eteService.getCourseCompletionEvent(eventId)
      }.also {
        assertThat(it.message).contains("Course completion event")
        assertThat(it.message).contains(eventId.toString())
      }
    }
  }

  @Nested
  inner class ProcessCourseCompletionOutcome {
    @Test
    fun `should create new appointment when appointmentIdToUpdate is null`() {
      val event = EteCourseCompletionEventEntity.valid()
      val outcome = CourseCompletionOutcomeDto.valid()

      every { eteCourseCompletionEventEntityRepository.findByIdOrNull(event.id) } returns event

      val appointmentToCreate = CreateAppointmentDto.valid()
      every {
        educationCourseCompletionMapper.toCreateAppointmentDto(event, outcome)
      } returns appointmentToCreate
      every { contextService.getUserName() } returns "admin-ui"

      eteService.processCourseCompletionOutcome(event.id, outcome)

      val triggerSlot = slot<AppointmentEventTrigger>()

      io.mockk.verify {
        appointmentService.createAppointment(appointmentToCreate, capture(triggerSlot))
      }

      assertThat(triggerSlot.captured.triggerType).isEqualTo(AppointmentEventTriggerType.ETE_COURSE_COMPLETION)
      assertThat(triggerSlot.captured.triggeredBy).isEqualTo("admin-ui")
    }

    @Test
    fun `should update existing appointment when appointmentIdToUpdate is present`() {
      val eventId = UUID.randomUUID()
      val appointmentId = 12345L
      val projectCode = "PRJ001"
      val event = EteCourseCompletionEventEntity.valid()

      val existingAppointment = AppointmentDto.valid()

      val outcome = CourseCompletionOutcomeDto.valid().copy(
        appointmentIdToUpdate = appointmentId,
        minutesToCredit = 90,
        contactOutcomeCode = "COMP",
        projectCode = projectCode,
      )

      every { eteCourseCompletionEventEntityRepository.findByIdOrNull(eventId) } returns event
      every { appointmentService.getAppointment(projectCode, appointmentId) } returns existingAppointment

      val updateAppointmentDto = UpdateAppointmentOutcomeDto.valid()
      every {
        educationCourseCompletionMapper.toUpdateAppointmentDto(
          eteCourseCompletionEventEntity = event,
          courseCompletionOutcome = outcome,
          existingAppointment = existingAppointment,
        )
      } returns updateAppointmentDto
      every { contextService.getUserName() } returns "admin-ui"

      eteService.processCourseCompletionOutcome(eventId, outcome)

      val triggerSlot = slot<AppointmentEventTrigger>()

      io.mockk.verify {
        appointmentService.updateAppointmentOutcome(
          projectCode = projectCode,
          update = updateAppointmentDto,
          trigger = capture(triggerSlot),
        )
      }

      assertThat(triggerSlot.captured.triggerType).isEqualTo(AppointmentEventTriggerType.ETE_COURSE_COMPLETION)
    }

    @Test
    fun `throws NotFoundException when event not found in processCourseCompletionOutcome`() {
      val eventId = UUID.randomUUID()
      val outcome = CourseCompletionOutcomeDto(
        crn = "X123456",
        deliusEventNumber = 10L,
        appointmentIdToUpdate = null,
        minutesToCredit = 120,
        contactOutcomeCode = "COMP",
        projectCode = "PRJ001",
      )

      every { eteCourseCompletionEventEntityRepository.findById(eventId) } returns empty()

      assertThrows<NotFoundException> {
        eteService.processCourseCompletionOutcome(eventId, outcome)
      }
    }
  }
}
