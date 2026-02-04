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
    val attributes = message.messageAttributes
    eteCourseEventEntityRepository.save(
      EteCourseEventEntity(
        id = UUID.randomUUID(),
        crn = attributes.crn,
        firstName = attributes.firstName,
        lastName = attributes.lastName,
        dateOfBirth = attributes.dateOfBirth,
        region = attributes.region,
        email = attributes.email,
        courseName = attributes.courseName,
        courseType = attributes.courseType,
        provider = attributes.provider,
        completionDateTime = attributes.completionDateTime,
        status = EteCourseEventStatus.fromMessage(attributes.status),
        totalTime = attributes.totalTime,
        expectedMinutes = attributes.expectedMinutes,
        externalId = attributes.externalReference,
      ),
    )
    if (attributes.status == EducationCourseCompletionStatus.Completed) {
      appointmentCreationService.createAppointments(
        educationCourseCompletionMapper.toCreateAppointmentsDto(message, projectCode = "N56CCTEST"),
        AppointmentEventTrigger(
          triggerType = AppointmentEventTriggerType.ETE_COURSE_COMPLETION,
          triggeredBy = "External ETE System: ${attributes.externalReference}",
        ),
      )
    }
  }
}
