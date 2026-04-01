package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionRecommendationDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionResolutionDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionResolutionTypeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.EteCourseCompletionEventDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.EteCourseCompletionResolutionStatusDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntityRepository.ResolutionStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventResolutionRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseCompletionMessage
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.EteMappers
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto
import java.time.OffsetDateTime
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
  companion object {
    const val ETE_ALLOWANCE_OF_TOTAL_REQUIREMENT = 0.3
  }

  fun recordCourseCompletionEvent(message: EducationCourseCompletionMessage) {
    eteCourseCompletionEventEntityRepository.save(eteMapper.toCourseCompletionEventEntity(message))
  }

  fun getPassedCourseCompletionEvents(
    providerCode: String,
    pduId: UUID?,
    offices: List<String>?,
    resolutionStatus: EteCourseCompletionResolutionStatusDto?,
    fromDate: OffsetDateTime?,
    toDate: OffsetDateTime?,
    pageable: Pageable,
  ): Page<EteCourseCompletionEventDto> {
    val officesNormalised = offices ?: emptyList()

    val page = eteCourseCompletionEventEntityRepository.findAllPassedWithFilters(
      providerCode,
      pduId,
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

  @Transactional
  fun recordCourseCompletionResolution(
    eteCourseCompletionEventId: UUID,
    courseCompletionResolution: CourseCompletionResolutionDto,
  ) {
    val courseCompletionEvent = getEventOrError(eteCourseCompletionEventId)

    when (eteValidationService.validateCourseCompletionResolution(courseCompletionResolution, courseCompletionEvent)) {
      EteValidationService.ValidationResult.EXISTING_IDENTICAL_RESOLUTION -> return
      EteValidationService.ValidationResult.VALID -> Unit
    }

    when (courseCompletionResolution.type) {
      CourseCompletionResolutionTypeDto.CREDIT_TIME -> creditTime(courseCompletionResolution, courseCompletionEvent)
      CourseCompletionResolutionTypeDto.DONT_CREDIT_TIME -> dontCreditTime(courseCompletionResolution, courseCompletionEvent)
    }
  }

  private fun creditTime(
    courseCompletionResolution: CourseCompletionResolutionDto,
    courseCompletionEvent: EteCourseCompletionEventEntity,
  ) {
    val resolutionId = UUID.randomUUID()
    val appointmentEventTrigger = AppointmentEventTrigger(
      triggerType = AppointmentEventTriggerType.ETE_COURSE_COMPLETION_RESOLUTION,
      triggeredBy = resolutionId.toString(),
    )

    val deliusAppointmentId = if (courseCompletionResolution.creditTimeDetails!!.appointmentIdToUpdate == null) {
      appointmentService.createAppointment(
        appointment = eteMapper.toCreateAppointmentDto(courseCompletionResolution),
        trigger = appointmentEventTrigger,
      )
    } else {
      val existingAppointment = appointmentService.getAppointment(
        projectCode = courseCompletionResolution.creditTimeDetails.projectCode,
        deliusAppointmentId = courseCompletionResolution.creditTimeDetails.appointmentIdToUpdate,
      )

      appointmentService.updateAppointmentOutcome(
        projectCode = existingAppointment.projectCode,
        update = eteMapper.toUpdateAppointmentDto(
          courseCompletionResolution = courseCompletionResolution,
          existingAppointment = existingAppointment,
        ),
        trigger = appointmentEventTrigger,
      )

      courseCompletionResolution.creditTimeDetails.appointmentIdToUpdate
    }

    eteCourseCompletionEventResolutionRepository.save(
      eteMapper.toResolutionEntityForCreditTime(
        id = resolutionId,
        courseCompletionEvent = courseCompletionEvent,
        courseCompletionResolution = courseCompletionResolution,
        deliusAppointmentId = deliusAppointmentId,
      ),
    )
  }

  private fun dontCreditTime(
    courseCompletionResolution: CourseCompletionResolutionDto,
    courseCompletionEvent: EteCourseCompletionEventEntity,
  ) {
    eteCourseCompletionEventResolutionRepository.save(
      eteMapper.toResolutionEntityForDontCreditTime(
        id = UUID.randomUUID(),
        courseCompletionEvent = courseCompletionEvent,
        courseCompletionResolution = courseCompletionResolution,
      ),
    )
  }

  private fun getEventOrError(id: UUID) = eteCourseCompletionEventEntityRepository.findByIdOrNull(id)
    ?: throw NotFoundException("Course completion event", id.toString())
}
