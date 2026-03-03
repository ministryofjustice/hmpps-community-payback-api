package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.EteCourseCompletionEventDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.EteCourseCompletionResolutionStatusDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntityRepository.ResolutionStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventResolutionRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseCompletionMessage
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.EteMappers
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Service
class EteService(
  private val eteMapper: EteMappers,
  private val eteCourseCompletionEventEntityRepository: EteCourseCompletionEventEntityRepository,
  private val eteCourseCompletionEventResolutionRepository: EteCourseCompletionEventResolutionRepository,
  private val contextService: ContextService,
  private val appointmentService: AppointmentService,
  private val eteValidationService: EteValidationService,
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
        office = attributes.office,
        email = attributes.email,
        courseName = attributes.courseName,
        courseType = attributes.courseType,
        provider = attributes.provider,
        completionDate = attributes.completionDate,
        status = EteCourseCompletionEventStatus.fromMessage(attributes.status),
        totalTimeMinutes = attributes.totalTimeMinutes,
        expectedTimeMinutes = attributes.expectedTimeMinutes,
        externalReference = attributes.externalReference,
        attempts = attributes.attempts,
      ),
    )
  }

  fun getEteCourseCompletionEvents(
    providerCode: String,
    fromDate: LocalDate?,
    toDate: LocalDate?,
    offices: List<String>?,
    resolutionStatus: EteCourseCompletionResolutionStatusDto?,
    pageable: Pageable,
  ): Page<EteCourseCompletionEventDto> {
    val region = providerCodeToRegionName[providerCode] ?: return Page.empty()
    val officesNormalised = offices ?: emptyList()

    val page = eteCourseCompletionEventEntityRepository.findAllWithFilters(
      region,
      officesNormalised.size,
      officesNormalised,
      resolutionStatus = when (resolutionStatus) {
        EteCourseCompletionResolutionStatusDto.Resolved -> ResolutionStatus.RESOLVED
        EteCourseCompletionResolutionStatusDto.Unresolved -> ResolutionStatus.UNRESOLVED
        null -> ResolutionStatus.ANY
      },
      fromDate,
      toDate,
      pageable,
    )

    return page.map { it.toDto() }
  }

  fun getCourseCompletionEvent(id: UUID) = eteCourseCompletionEventEntityRepository.findById(id).orElseThrow {
    NotFoundException("Course completion event", id.toString())
  }.toDto()

  @Transactional
  fun processCourseCompletionOutcome(
    eteCourseCompletionEventId: UUID,
    courseCompletionOutcome: CourseCompletionOutcomeDto,
  ) {
    eteValidationService.validateCourseCompletionOutcome(courseCompletionOutcome)

    val courseCompletionEvent = eteCourseCompletionEventEntityRepository.findByIdOrNull(eteCourseCompletionEventId)
      ?: throw NotFoundException("Course completion event", eteCourseCompletionEventId.toString())

    val createAppointment = courseCompletionOutcome.appointmentIdToUpdate == null

    val deliusAppointmentId = if (createAppointment) {
      createAppointment(
        courseCompletionEvent = courseCompletionEvent,
        courseCompletionOutcome = courseCompletionOutcome,
      )
    } else {
      updateExistingAppointment(
        courseCompletionEvent = courseCompletionEvent,
        courseCompletionOutcome = courseCompletionOutcome,
      )
    }

    val res = eteMapper.toResolutionEntity(
      courseCompletionEvent = courseCompletionEvent,
      courseCompletionOutcome = courseCompletionOutcome,
      deliusAppointmentId = deliusAppointmentId,
      deliusAppointmentCreated = createAppointment,
    )
    eteCourseCompletionEventResolutionRepository.save(
      res,
    )
  }

  private fun updateExistingAppointment(
    courseCompletionEvent: EteCourseCompletionEventEntity,
    courseCompletionOutcome: CourseCompletionOutcomeDto,
  ): Long {
    val appointmentIdToUpdate = courseCompletionOutcome.appointmentIdToUpdate!!

    val existingAppointment = appointmentService.getAppointment(
      projectCode = courseCompletionOutcome.projectCode,
      appointmentId = appointmentIdToUpdate,
    )

    val update = eteMapper.toUpdateAppointmentDto(
      eteCourseCompletionEventEntity = courseCompletionEvent,
      courseCompletionOutcome = courseCompletionOutcome,
      existingAppointment = existingAppointment,
    )

    appointmentService.updateAppointmentOutcome(
      projectCode = courseCompletionOutcome.projectCode,
      update = update,
      trigger = buildEventTrigger(),
    )

    return appointmentIdToUpdate
  }

  private fun createAppointment(
    courseCompletionEvent: EteCourseCompletionEventEntity,
    courseCompletionOutcome: CourseCompletionOutcomeDto,
  ): Long {
    val appointment = eteMapper.toCreateAppointmentDto(
      eteCourseCompletionEventEntity = courseCompletionEvent,
      courseCompletionOutcome = courseCompletionOutcome,
    )

    return appointmentService.createAppointment(
      appointment = appointment,
      trigger = buildEventTrigger(),
    )
  }

  private fun buildEventTrigger() = AppointmentEventTrigger(
    triggeredAt = OffsetDateTime.now(),
    triggerType = AppointmentEventTriggerType.ETE_COURSE_COMPLETION,
    triggeredBy = contextService.getUserName(),
  )
}
