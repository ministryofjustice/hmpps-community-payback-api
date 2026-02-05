package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseEventEntityRepository
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

@ExtendWith(MockKExtension::class)
class EteServiceTest {

  @RelaxedMockK
  lateinit var eteCourseEventEntityRepository: EteCourseEventEntityRepository

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
      every { eteCourseEventEntityRepository.save(capture(entityCaptor)) } returnsArgument 0

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
      every { eteCourseEventEntityRepository.save(capture(entityCaptor)) } returnsArgument 0

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
}
