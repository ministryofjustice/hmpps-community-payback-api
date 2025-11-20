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
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.HourMinuteDuration
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseEventStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.CommunityCampusCourse
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.CommunityCampusCourseCompletionMessage
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.CommunityCampusCourseCompletionStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.CommunityCampusPerson
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.EteService
import java.time.Duration

@ExtendWith(MockKExtension::class)
class EteServiceTest {

  @RelaxedMockK
  lateinit var eteCourseEventEntityRepository: EteCourseEventEntityRepository

  @InjectMockKs
  private lateinit var service: EteService

  @Nested
  inner class HandleCommunityCampusMessage {

    @Test
    fun `create ete course event entry`() {
      val entityCaptor = slot<EteCourseEventEntity>()
      every { eteCourseEventEntityRepository.save(capture(entityCaptor)) } returnsArgument 0

      service.handleCommunityCampusMessage(
        CommunityCampusCourseCompletionMessage.valid().copy(
          person = CommunityCampusPerson.valid().copy(
            crn = "CRN01",
          ),
          course = CommunityCampusCourse.valid().copy(
            courseName = "The course name",
            totalTime = HourMinuteDuration(duration = Duration.ofMinutes(70)),
            attempts = 55,
            status = CommunityCampusCourseCompletionStatus.Failed,
          ),
        ),
      )

      assertThat(entityCaptor.isCaptured).isTrue
      val persistedEntity = entityCaptor.captured
      assertThat(persistedEntity.crn).isEqualTo("CRN01")
      assertThat(persistedEntity.courseName).isEqualTo("The course name")
      assertThat(persistedEntity.totalTimeMinutes).isEqualTo(70)
      assertThat(persistedEntity.attempts).isEqualTo(55)
      assertThat(persistedEntity.status).isEqualTo(EteCourseEventStatus.FAILED)
    }
  }
}
