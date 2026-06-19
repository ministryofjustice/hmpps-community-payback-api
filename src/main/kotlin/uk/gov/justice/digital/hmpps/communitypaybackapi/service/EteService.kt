package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.slf4j.LoggerFactory
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
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.EteCourseCompletionResolutionStatusDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.EteCourseCompletionShowCourseFailuresDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntityRepository.CourseFailureFilter
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntityRepository.ResolutionStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventResolutionRepository
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
  private val contextService: ContextService,
  private val springEventPublisher: SpringEventPublisher,
  private val courseCompletionAutoResolutionService: CourseCompletionAutoResolutionService,

  // The dates below are temporary, during the initial stages of private beta
  @Value("\${course.completions.available.from:#{null}}") private val courseCompletionsAvailableFrom: OffsetDateTime?,
  @Value("\${course.completions.available.to:#{null}}") private val courseCompletionsAvailableTo: OffsetDateTime?,
  // London - N07
  @Value("\${course.completions.london.available.from:#{null}}") private val londonAvailableFrom: OffsetDateTime?,
  @Value("\${course.completions.london.available.to:#{null}}") private val londonAvailableTo: OffsetDateTime?,
  // South Central - N59
  @Value("\${course.completions.south-central.available.from:#{null}}") private val southCentralAvailableFrom: OffsetDateTime?,
  @Value("\${course.completions.south-central.available.to:#{null}}") private val southCentralAvailableTo: OffsetDateTime?,

  @Value("\${course.completions.auto-resolution.enabled:false}")
  private val courseCompletionAutoResolutionEnabled: Boolean,
) {
  companion object {
    const val ETE_ALLOWANCE_OF_TOTAL_REQUIREMENT = 0.3
    private val log = LoggerFactory.getLogger(EteService::class.java)
  }

  @Suppress("TooGenericExceptionCaught")
  fun recordCourseCompletionEvent(message: EducationCourseCompletionMessage) {
    val event = eteCourseCompletionEventEntityRepository.save(eteMapper.toCourseCompletionEventEntity(message))

    if (courseCompletionAutoResolutionEnabled) {
      try {
        courseCompletionAutoResolutionService.resolveAndPersistDraft(event)
      } catch (e: Exception) {
        log.warn("Auto-resolution failed for event {} — draft will be empty; user must resolve manually", event.id, e)
      }
    }

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
    showCourseFailures: EteCourseCompletionShowCourseFailuresDto?,
    externalReference: String?,
    fromDate: OffsetDateTime?,
    toDate: OffsetDateTime?,
    pageable: Pageable,
  ): Page<EteCourseCompletionEventDto> {
    val officesNormalised = offices ?: emptyList()

    val (effectiveAvailableFromDate, effectiveAvailableToDate) = getEffectiveAvailableDates(providerCode)

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
      courseFailures = when (showCourseFailures) {
        null, EteCourseCompletionShowCourseFailuresDto.No -> CourseFailureFilter.HIDE
        EteCourseCompletionShowCourseFailuresDto.Yes -> CourseFailureFilter.SHOW_ALL
        EteCourseCompletionShowCourseFailuresDto.OnlyWhenMaxAttemptsReached -> CourseFailureFilter.SHOW_ONLY_WHEN_MAX_ATTEMPTS_REACHED
      },
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

  fun getCourseCompletionBlock(id: UUID, blockSize: Int): List<EteCourseCompletionEventDto> {
    require(blockSize > 0) { "blockSize must be greater than 0" }
    val event = getEventOrError(id)
    val attempts = event.attempts ?: 1

    val startAttempt = ((attempts - 1) / blockSize) * blockSize + 1
    val endAttempt = startAttempt + blockSize - 1

    return eteCourseCompletionEventEntityRepository.findBlock(
      event.pdu.providerCode,
      event.externalReference,
      startAttempt,
      endAttempt,
    ).map { it.toDto() }
  }

  fun getCourseCompletionRecommendation(id: UUID): CourseCompletionRecommendationDto? {
    val courseCompletionEvent = getEventOrError(id)

    val email = courseCompletionEvent.email

    val crn: String? =
      eteCourseCompletionEventResolutionRepository
        .findFirstByEteCourseCompletionEventEmailOrderByCreatedAtDesc(email)
        ?.crn

    return CourseCompletionRecommendationDto(crn)
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

  // The dates below are temporary, during the initial stages of private beta
  private fun getEffectiveAvailableDates(providerCode: String): Pair<OffsetDateTime?, OffsetDateTime?> = when (providerCode) {
    "N07" -> Pair(londonAvailableFrom, londonAvailableTo ?: courseCompletionsAvailableTo)
    "N59" -> Pair(southCentralAvailableFrom, southCentralAvailableTo ?: courseCompletionsAvailableTo)
    else -> Pair(courseCompletionsAvailableFrom, courseCompletionsAvailableTo)
  }
}
