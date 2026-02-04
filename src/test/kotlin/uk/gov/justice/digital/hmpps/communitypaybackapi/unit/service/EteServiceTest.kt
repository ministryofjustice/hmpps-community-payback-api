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
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseEventStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalDateTime
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseCompletionMessage
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseCompletionStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseCourse
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCoursePerson
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentCreationService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.EteService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.EducationCourseCompletionMapper
import java.time.LocalDate
import java.time.LocalDateTime

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
      val entityCaptor = slot<EteCourseEventEntity>()
      every { eteCourseEventEntityRepository.save(capture(entityCaptor)) } returnsArgument 0

      service.handleEducationCourseMessage(
        EducationCourseCompletionMessage.valid().copy(
          externalReference = "EXT123",
          person = EducationCoursePerson.valid().copy(
            crn = "CRN01",
            firstName = "John",
            lastName = "Doe",
            dateOfBirth = LocalDate.of(1990, 5, 15),
            region = "London",
            email = "john.doe@example.com",
          ),
          course = EducationCourseCourse.valid().copy(
            courseName = "The course name",
            courseType = "Online",
            provider = "Training Provider Inc.",
            totalTime = 70,
            expectedMinutes = 120,
            status = EducationCourseCompletionStatus.Failed,
            completionDateTime = LocalDateTime.of(2026, 1, 1, 10, 0),
          ),
        ),
      )

      assertThat(entityCaptor.isCaptured).isTrue
      val persistedEntity = entityCaptor.captured

      // Person data assertions
      assertThat(persistedEntity.crn).isEqualTo("CRN01")
      assertThat(persistedEntity.firstName).isEqualTo("John")
      assertThat(persistedEntity.lastName).isEqualTo("Doe")
      assertThat(persistedEntity.dateOfBirth).isEqualTo(LocalDate.of(1990, 5, 15))
      assertThat(persistedEntity.region).isEqualTo("London")
      assertThat(persistedEntity.email).isEqualTo("john.doe@example.com")

      // Course data assertions
      assertThat(persistedEntity.courseName).isEqualTo("The course name")
      assertThat(persistedEntity.courseType).isEqualTo("Online")
      assertThat(persistedEntity.provider).isEqualTo("Training Provider Inc.")
      assertThat(persistedEntity.totalTime).isEqualTo(70)
      assertThat(persistedEntity.expectedMinutes).isEqualTo(120)
      assertThat(persistedEntity.status).isEqualTo(EteCourseEventStatus.FAILED)
      assertThat(persistedEntity.completionDateTime).isEqualTo("2026-01-01T10:00")

      // External ID assertion
      assertThat(persistedEntity.externalId).isEqualTo("EXT123")

      // Verify UUID is generated
      assertThat(persistedEntity.id).isNotNull
    }

    @Test
    fun `create ete course event entry with completed status`() {
      val entityCaptor = slot<EteCourseEventEntity>()
      every { eteCourseEventEntityRepository.save(capture(entityCaptor)) } returnsArgument 0

      service.handleEducationCourseMessage(
        EducationCourseCompletionMessage.valid().copy(
          externalReference = "EXT456",
          person = EducationCoursePerson.valid().copy(
            crn = "CRN02",
            firstName = "Jane",
            lastName = "Smith",
            dateOfBirth = LocalDate.of(1985, 8, 22),
            region = "Manchester",
            email = "jane.smith@example.com",
          ),
          course = EducationCourseCourse.valid().copy(
            courseName = "Advanced Course",
            courseType = "In-person",
            provider = "Education Corp",
            totalTime = 150,
            expectedMinutes = 150,
            status = EducationCourseCompletionStatus.Completed,
            completionDateTime = randomLocalDateTime(),
          ),
        ),
      )

      assertThat(entityCaptor.isCaptured).isTrue
      val persistedEntity = entityCaptor.captured

      assertThat(persistedEntity.crn).isEqualTo("CRN02")
      assertThat(persistedEntity.status).isEqualTo(EteCourseEventStatus.COMPLETED)
      assertThat(persistedEntity.totalTime).isEqualTo(150) // 2 hours 30 minutes = 150 minutes
      assertThat(persistedEntity.expectedMinutes).isEqualTo(150)
      assertThat(persistedEntity.externalId).isEqualTo("EXT456")

      // ensure appointment is created
    }
  }
}
