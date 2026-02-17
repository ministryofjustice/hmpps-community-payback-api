package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAppointmentsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.EteCourseCompletionEventDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseEventCompletionMessageStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseCompletionMessage
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
  private val appointmentUpdateService: AppointmentUpdateService,
  private val appointmentRetrievalService: AppointmentRetrievalService,
  private val contextService: ContextService,
) {
  private val providerCodeToRegionName = mapOf(
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
        status = EteCourseEventCompletionMessageStatus.fromMessage(attributes.status),
        totalTimeMinutes = attributes.totalTimeMinutes,
        expectedTimeMinutes = attributes.expectedTimeMinutes,
        externalReference = attributes.externalReference,
        attempts = attributes.attempts,
      ),
    )
  }

  fun getEteCourseCompletionEvents(providerCode: String, fromDate: LocalDate?, toDate: LocalDate?, pageable: Pageable): Page<EteCourseCompletionEventDto> {
    val region = providerCodeToRegionName[providerCode] ?: return Page.empty()
    return eteCourseCompletionEventEntityRepository.findByRegionAndDateRange(region, fromDate, toDate, pageable).map { it.toDto() }
  }

  fun getCourseCompletionEvent(id: UUID) = eteCourseCompletionEventEntityRepository.findById(id).orElseThrow {
    NotFoundException("Course completion event", id.toString())
  }.toDto()

  @Transactional
  fun processCourseCompletionOutcome(
    eteCourseCompletionEventId: UUID,
    courseCompletionOutcome: CourseCompletionOutcomeDto,
  ) {
    val eteEvent = eteCourseCompletionEventEntityRepository.findById(eteCourseCompletionEventId).orElseThrow {
      NotFoundException("Course completion event", eteCourseCompletionEventId.toString())
    }

    val trigger = AppointmentEventTrigger(
      triggeredAt = OffsetDateTime.now(),
      triggerType = AppointmentEventTriggerType.ETE_COURSE_COMPLETION,
      triggeredBy = contextService.getUserName(),
    )

    val appointmentIdToUpdate = courseCompletionOutcome.appointmentIdToUpdate
    if (appointmentIdToUpdate != null) {
      val existingAppointment = appointmentRetrievalService.getAppointment(
        projectCode = courseCompletionOutcome.projectCode,
        appointmentId = appointmentIdToUpdate,
      )

      val update = UpdateAppointmentOutcomeDto(
        deliusId = existingAppointment.id,
        deliusVersionToUpdate = existingAppointment.version,
        startTime = existingAppointment.startTime,
        endTime = existingAppointment.startTime.plusMinutes(courseCompletionOutcome.minutesToCredit),
        contactOutcomeCode = courseCompletionOutcome.contactOutcome,
        attendanceData = EducationCourseCompletionMapper.DefaultEducationCourseCompletionAttendanceData.createAttendanceData(),
        enforcementData = null,
        supervisorOfficerCode = existingAppointment.supervisorOfficerCode,
        notes = "Ete course completed: ${eteEvent.courseName}",
        formKeyToDelete = null,
        alertActive = existingAppointment.alertActive,
        sensitive = existingAppointment.sensitive,
      )

      appointmentUpdateService.updateAppointmentOutcome(
        projectCode = courseCompletionOutcome.projectCode,
        update = update,
        trigger = trigger,
      )
    } else {
      val appointment = educationCourseCompletionMapper.toCreateAppointmentDto(eteEvent, courseCompletionOutcome.crn)
      val adjustedAppointment = appointment.copy(
        crn = courseCompletionOutcome.crn,
        endTime = appointment.startTime.plusMinutes(courseCompletionOutcome.minutesToCredit),
        contactOutcomeCode = courseCompletionOutcome.contactOutcome,
      )

      val createAppointments = CreateAppointmentsDto(
        projectCode = courseCompletionOutcome.projectCode,
        appointments = listOf(adjustedAppointment),
      )

      appointmentCreationService.createAppointments(
        createAppointments = createAppointments,
        trigger = trigger,
      )
    }
  }
}
