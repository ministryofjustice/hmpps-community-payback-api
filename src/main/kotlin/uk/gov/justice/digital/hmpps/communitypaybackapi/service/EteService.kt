package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseEventStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseCompletionMessage
import java.util.UUID

@Service
class EteService(
  private val eteCourseEventEntityRepository: EteCourseEventEntityRepository,
) {

  @Transactional
  fun handleEducationCourseMessage(message: EducationCourseCompletionMessage) {
    eteCourseEventEntityRepository.save(
      EteCourseEventEntity(
        id = UUID.randomUUID(),
        crn = message.person.crn,
        firstName = message.person.firstName,
        lastName = message.person.lastName,
        dateOfBirth = message.person.dateOfBirth,
        region = message.person.region,
        email = message.person.email,
        courseName = message.course.courseName,
        courseType = message.course.courseType,
        provider = message.course.provider,
        status = EteCourseEventStatus.fromMessage(message.course.status),
        totalTime = message.course.totalTime,
        expectedMinutes = message.course.expectedMinutes,
        externalId = message.externalId,
      ),
    )
  }
}
