package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseEventStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseCompletionMessage
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseCompletionStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.EducationCourseCompletionMapper
import java.time.OffsetDateTime
import java.util.UUID

@Service
class EteService(
  private val educationCourseCompletionMapper: EducationCourseCompletionMapper,
  private val eteCourseCompletionEventEntityRepository: EteCourseCompletionEventEntityRepository,
  private val appointmentCreationService: AppointmentCreationService,
) {

  @Transactional
  fun handleEducationCourseCompletionMessage(message: EducationCourseCompletionMessage) {
    val attributes = message.messageAttributes
    eteCourseCompletionEventEntityRepository.save(
      EteCourseCompletionEventEntity(
        id = UUID.randomUUID(),
        firstName = attributes.firstName,
        lastName = attributes.lastName,
        dateOfBirth = attributes.dateOfBirth,
        region = attributes.region,
        email = attributes.email,
        courseName = attributes.courseName,
        courseType = attributes.courseType,
        provider = attributes.provider,
        completionDate = attributes.completionDate,
        status = EteCourseEventStatus.fromMessage(attributes.status),
        totalTimeMinutes = attributes.totalTimeMinutes,
        expectedTimeMinutes = attributes.expectedTimeMinutes,
        externalReference = attributes.externalReference,
        attempts = attributes.attempts,
      ),
    )
    if (attributes.status == EducationCourseCompletionStatus.Completed) {
      appointmentCreationService.createAppointments(
        educationCourseCompletionMapper.toCreateAppointmentsDto(message, projectCode = "N56CCTEST"),
        AppointmentEventTrigger(
          triggeredAt = OffsetDateTime.now(),
          triggerType = AppointmentEventTriggerType.ETE_COURSE_COMPLETION,
          triggeredBy = "External ETE System: ${attributes.externalReference}",
        ),
      )
    }
  }
}
