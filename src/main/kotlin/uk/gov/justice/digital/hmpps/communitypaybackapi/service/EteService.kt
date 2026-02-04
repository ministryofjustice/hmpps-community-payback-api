package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseEventStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseCompletionMessage
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseCompletionStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.EducationCourseCompletionMapper
import java.util.UUID

@Service
class EteService(
  private val educationCourseCompletionMapper: EducationCourseCompletionMapper,
  private val eteCourseEventEntityRepository: EteCourseEventEntityRepository,
  private val appointmentCreationService: AppointmentCreationService,
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
        completionDateTime = message.course.completionDateTime,
        status = EteCourseEventStatus.fromMessage(message.course.status),
        totalTime = message.course.totalTime,
        expectedMinutes = message.course.expectedMinutes,
        externalId = message.externalReference,
      ),
    )
    if (message.course.status == EducationCourseCompletionStatus.Completed) {
      appointmentCreationService.createAppointments(
        educationCourseCompletionMapper.toCreateAppointmentsDto(message, projectCode = "N56CCTEST"),
        AppointmentEventTrigger(
          triggerType = AppointmentEventTriggerType.ETE_COURSE_COMPLETION,
          triggeredBy = "External ETE System: ${message.externalReference}",
        ),
      )
    }
  }
}
