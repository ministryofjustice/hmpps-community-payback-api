package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.badRequest
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionRecommendationDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionResolutionDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionResolutionTypeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.DeliusAppointmentIdDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.EteCourseCompletionEventDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.EteCourseCompletionEventStatusDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.EteCourseCompletionResolutionStatusDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntityRepository.ResolutionStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventResolutionRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseCompletionMessage
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.CourseCompletionProcessedEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.CourseCompletionReceivedEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.SpringEventPublisher
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
  private val contextService: ContextService,
  private val springEventPublisher: SpringEventPublisher,

  // The dates below are temporary, during the initial stages of private beta
  @Value("\${course.completions.available.from:#{null}") private val courseCompletionsAvailableFrom: OffsetDateTime?,
  @Value("\${course.completions.available.to:#{null}}") private val courseCompletionsAvailableTo: OffsetDateTime?,
  // London - N07
  @Value("\${course.completions.london.available.from:#{null}}") private val londonAvailableFrom: OffsetDateTime?,
  @Value("\${course.completions.london.available.to:#{null}}") private val londonAvailableTo: OffsetDateTime?,
  // South Central - N59
  @Value("\${course.completions.south-central.available.from:#{null}}") private val southCentralAvailableFrom: OffsetDateTime?,
  @Value("\${course.completions.south-central.available.to:#{null}}") private val southCentralAvailableTo: OffsetDateTime?,
) {
  companion object {
    const val ETE_ALLOWANCE_OF_TOTAL_REQUIREMENT = 0.3
  }

  fun recordCourseCompletionEvent(message: EducationCourseCompletionMessage) {
    val event = eteCourseCompletionEventEntityRepository.save(eteMapper.toCourseCompletionEventEntity(message))

    springEventPublisher.publishEvent(
      CourseCompletionReceivedEvent(
        attempts = event.attempts,
        courseName = event.courseName,
        courseType = event.courseType,
        provider = event.provider,
        region = event.region,
        triggeredAt = event.receivedAt,
        triggeredBy = event.externalReference,
      ),
    )
  }

  fun getCourseCompletionEvents(
    providerCode: String,
    pduId: UUID?,
    offices: List<String>?,
    resolutionStatus: EteCourseCompletionResolutionStatusDto?,
    completionStatus: EteCourseCompletionEventStatusDto,
    attempts: Int?,
    externalReference: String?,
    fromDate: OffsetDateTime?,
    toDate: OffsetDateTime?,
    pageable: Pageable,
  ): Page<EteCourseCompletionEventDto> {
    val officesNormalised = offices ?: emptyList()

    val effectiveAvailableFromDate = when (providerCode) {
      "N07" -> londonAvailableFrom
      "N59" -> southCentralAvailableFrom
      else -> courseCompletionsAvailableFrom
    }

    val effectiveAvailableToDate = when (providerCode) {
      "N07" -> londonAvailableTo ?: courseCompletionsAvailableTo
      "N59" -> southCentralAvailableTo ?: courseCompletionsAvailableTo
      else -> courseCompletionsAvailableTo
    }

    val page = eteCourseCompletionEventEntityRepository.findAllWithFilters(
      providerCode,
      pduId,
      officesNormalised.size,
      officesNormalised,
      resolutionStatus = when (resolutionStatus) {
        EteCourseCompletionResolutionStatusDto.Resolved -> ResolutionStatus.RESOLVED
        EteCourseCompletionResolutionStatusDto.Unresolved -> ResolutionStatus.UNRESOLVED
        null -> ResolutionStatus.ANY
      },
      completionStatus = when (completionStatus) {
        EteCourseCompletionEventStatusDto.Passed -> EteCourseCompletionEventStatus.PASSED
        EteCourseCompletionEventStatusDto.Failed -> EteCourseCompletionEventStatus.FAILED
      },
      attempts,
      externalReference,
      fromDate,
      toDate,
      effectiveAvailableFromDate,
      effectiveAvailableToDate,
      pageable,
    )

    return page.map { it.toDto() }
  }

  fun getCourseCompletionEvent(id: UUID) = eteCourseCompletionEventEntityRepository.findByIdOrNull(id)?.toDto()

  fun getCourseCompletionRecommendation(id: UUID): CourseCompletionRecommendationDto? {
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

    springEventPublisher.publishEvent(
      CourseCompletionProcessedEvent(
        crn = courseCompletionResolution.crn,
        externalReference = courseCompletionEvent.externalReference,
        resolutionType = courseCompletionResolution.type,
        triggeredAt = OffsetDateTime.now(),
        triggeredBy = contextService.getUserName(),
      ),
    )
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
        appointment = eteMapper.toCreateAppointmentDto(
          courseCompletionResolution = courseCompletionResolution,
          courseCompletionEvent = courseCompletionEvent,
        ),
        trigger = appointmentEventTrigger,
      )
    } else {
      val appointmentId = DeliusAppointmentIdDto(
        projectCode = courseCompletionResolution.creditTimeDetails.projectCode,
        deliusAppointmentId = courseCompletionResolution.creditTimeDetails.appointmentIdToUpdate,
      )
      val existingAppointment = appointmentService.getAppointment(appointmentId) ?: badRequest("Appointment not found with ID '$appointmentId'")

      appointmentService.updateAppointment(
        existingAppointment = existingAppointment,
        update = eteMapper.toUpdateAppointmentDto(
          courseCompletionResolution = courseCompletionResolution,
          courseCompletionEvent = courseCompletionEvent,
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

  private fun getEventOrError(id: UUID) = eteCourseCompletionEventEntityRepository.findByIdOrNull(id) ?: error("Can't find course completion event $id")
}
