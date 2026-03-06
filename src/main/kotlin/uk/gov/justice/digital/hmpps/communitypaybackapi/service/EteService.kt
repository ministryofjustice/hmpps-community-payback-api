package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionRecommendationDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.EteCourseCompletionEventDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.EteCourseCompletionResolutionStatusDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntityRepository.ResolutionStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventResolutionRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseCompletionMessage
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.EteMappers
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto
import java.time.LocalDate
import java.util.UUID

@Service
class EteService(
  private val eteMapper: EteMappers,
  private val eteCourseCompletionEventEntityRepository: EteCourseCompletionEventEntityRepository,
  private val eteCourseCompletionEventResolutionRepository: EteCourseCompletionEventResolutionRepository,
  private val appointmentService: AppointmentService,
  private val eteValidationService: EteValidationService,
  private val projectService: ProjectService,
) {
  private val logger = LoggerFactory.getLogger(EteService::class.java)

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

  fun recordCourseCompletionEvent(message: EducationCourseCompletionMessage) {
    eteCourseCompletionEventEntityRepository.save(eteMapper.toCourseCompletionEventEntity(message))
  }

  fun getCourseCompletionEvents(
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

  fun getCourseCompletionEvent(id: UUID) = getEventOrError(id).toDto()

  @Transactional
  fun recordCourseCompletionOutcome(
    eteCourseCompletionEventId: UUID,
    courseCompletionOutcome: CourseCompletionOutcomeDto,
  ) {
    val resolutionId = UUID.randomUUID()
    val courseCompletionEvent = getEventOrError(eteCourseCompletionEventId)

    when (eteValidationService.validateCourseCompletionOutcome(courseCompletionOutcome, courseCompletionEvent)) {
      EteValidationService.ValidationResult.EXISTING_IDENTICAL_RESOLUTION -> return
      EteValidationService.ValidationResult.VALID -> Unit
    }

    val appointmentEventTrigger = AppointmentEventTrigger(
      triggerType = AppointmentEventTriggerType.ETE_COURSE_COMPLETION_RESOLUTION,
      triggeredBy = resolutionId.toString(),
    )

    val deliusAppointmentId = if (courseCompletionOutcome.appointmentIdToUpdate == null) {
      createAppointment(
        trigger = appointmentEventTrigger,
        courseCompletionOutcome = courseCompletionOutcome,
      )
    } else {
      updateExistingAppointment(
        trigger = appointmentEventTrigger,
        courseCompletionOutcome = courseCompletionOutcome,
      )
    }

    eteCourseCompletionEventResolutionRepository.save(
      eteMapper.toResolutionEntity(
        id = resolutionId,
        courseCompletionEvent = courseCompletionEvent,
        courseCompletionOutcome = courseCompletionOutcome,
        deliusAppointmentId = deliusAppointmentId,
      ),
    )
  }

  fun getCourseCompletionRecommendation(id: UUID): CourseCompletionRecommendationDto {
    val courseCompletionEvent = getEventOrError(id)

    val email = courseCompletionEvent.email
    val courseName = courseCompletionEvent.courseName
    val office = courseCompletionEvent.office

    val crn: String? =
      eteCourseCompletionEventResolutionRepository
        .findFirstByEteCourseCompletionEventEmailOrderByCreatedAtDesc(email)
        ?.crn

    val projectCode: String? =
      eteCourseCompletionEventResolutionRepository
        .findFirstByEteCourseCompletionEventOfficeAndEteCourseCompletionEventCourseNameOrderByCreatedAtDesc(office, courseName)
        ?.projectCode

    val project = projectCode?.let { projectService.getProject(it) }

    return CourseCompletionRecommendationDto(crn, project)
  }

  private fun updateExistingAppointment(
    trigger: AppointmentEventTrigger,
    courseCompletionOutcome: CourseCompletionOutcomeDto,
  ): Long {
    val existingAppointment = appointmentService.getAppointment(
      projectCode = courseCompletionOutcome.projectCode,
      appointmentId = courseCompletionOutcome.appointmentIdToUpdate!!,
    )

    appointmentService.updateAppointmentOutcome(
      projectCode = existingAppointment.projectCode,
      update = eteMapper.toUpdateAppointmentDto(
        courseCompletionOutcome = courseCompletionOutcome,
        existingAppointment = existingAppointment,
      ),
      trigger = trigger,
    )

    return courseCompletionOutcome.appointmentIdToUpdate
  }

  private fun createAppointment(
    trigger: AppointmentEventTrigger,
    courseCompletionOutcome: CourseCompletionOutcomeDto,
  ): Long = appointmentService.createAppointment(
    appointment = eteMapper.toCreateAppointmentDto(courseCompletionOutcome),
    trigger = trigger,
  )

  private fun getEventOrError(id: UUID) = eteCourseCompletionEventEntityRepository.findByIdOrNull(id)
    ?: throw NotFoundException("Course completion event", id.toString())
}
