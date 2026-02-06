package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.EteCourseCompletionEventDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseEventStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseCompletionMessage
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseCompletionStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.EducationCourseCompletionMapper
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Service
class EteService(
  private val educationCourseCompletionMapper: EducationCourseCompletionMapper,
  private val eteCourseCompletionEventEntityRepository: EteCourseCompletionEventEntityRepository,
  private val appointmentCreationService: AppointmentCreationService,
) {

  private val regionToProviderCodeMap = mapOf(
    "N53" to "East Midlands",
    "N52" to "West Midlands",
    "N56" to "East of England",
    "N50" to "Greater Manchester",
    "N57" to "Kent, Surrey and Sussex",
    "N07" to "London",
    "N54" to "North East",
    "N51" to "North West",
    "N59" to "South Central",
    "N58" to "South West",
    "N03" to "Wales",
    "N55" to "Yorks & Humber",
  )

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

  fun getEteCourseCompletionEvents(providerCode: String, fromDate: LocalDate?, toDate: LocalDate?, pageable: Pageable): Page<EteCourseCompletionEventDto> {
    val region = regionToProviderCodeMap[providerCode] ?: return Page.empty()
    return eteCourseCompletionEventEntityRepository.findByRegionAndDateRange(region, fromDate, toDate, pageable).map { it.toDto() }
  }

  fun getCourseCompletionEvent(id: UUID) = eteCourseCompletionEventEntityRepository.findById(id).orElseThrow {
    NotFoundException("Course completion event", id.toString())
  }.toDto()
}
