package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseEventStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.CommunityCampusCourseCompletionMessage
import java.util.UUID

@Service
class EteService(
  private val eteCourseEventEntityRepository: EteCourseEventEntityRepository,
) {

  @Transactional
  fun handleCommunityCampusMessage(message: CommunityCampusCourseCompletionMessage) {
    eteCourseEventEntityRepository.save(
      EteCourseEventEntity(
        id = UUID.randomUUID(),
        crn = message.person.crn,
        courseName = message.course.courseName,
        totalTimeMinutes = message.course.totalTime.duration.toMinutes(),
        attempts = message.course.attempts,
        status = EteCourseEventStatus.Companion.fromMessage(message.course.status),
      ),
    )
  }
}
