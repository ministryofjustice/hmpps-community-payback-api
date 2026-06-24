package uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers

import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.formatForUser
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.toLocalDateTimeEuropeLondon
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.toLocalTimeEuropeLondon
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentBehaviourDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentWorkQualityDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AttendanceDataDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionDraftResolutionDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionResolutionDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.EteCourseCompletionEventDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.CommunityCampusPduEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionDraftResolutionEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntityRepository.CourseFailureFilter
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntityRepository.ResolutionStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventResolutionEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionResolution
import uk.gov.justice.digital.hmpps.communitypaybackapi.listener.EducationCourseCompletionMessage
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.ContextService
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class EteMappers(
  private val contextService: ContextService,
  private val contactOutcomeEntityRepository: ContactOutcomeEntityRepository,
  private val communityCampusPduEntityRepository: CommunityCampusPduEntityRepository,
  private val eteCourseCompletionEventEntityRepository: EteCourseCompletionEventEntityRepository,
) {

  companion object {
    val DEFAULT_APPOINTMENT_START_TIME: LocalTime = LocalTime.of(0, 1)
  }

  fun toCreateAppointmentDto(
    courseCompletionResolution: CourseCompletionResolutionDto,
    courseCompletionEvent: EteCourseCompletionEventEntity,
  ): CreateAppointmentDto {
    val creditTime = courseCompletionResolution.creditTimeDetails!!
    val appointmentTimesPair = calculateAppointmentTimes(creditTime.minutesToCredit, courseCompletionEvent)
    return CreateAppointmentDto(
      crn = courseCompletionResolution.crn!!,
      deliusEventNumber = creditTime.deliusEventNumber,
      allocationId = null,
      projectCode = creditTime.projectCode,
      date = creditTime.date,
      startTime = appointmentTimesPair.first,
      endTime = appointmentTimesPair.second,
      pickUpLocationCode = null,
      pickUpTime = null,
      contactOutcomeCode = creditTime.contactOutcomeCode,
      attendanceData = createAttendanceData(),
      supervisorOfficerCode = null,
      notes = buildNote(creditTime.notes, courseCompletionEvent),
      alertActive = creditTime.alertActive,
      sensitive = creditTime.sensitive,
    )
  }

  fun toUpdateAppointmentDto(
    courseCompletionResolution: CourseCompletionResolutionDto,
    courseCompletionEvent: EteCourseCompletionEventEntity,
    existingAppointment: AppointmentDto,
  ): UpdateAppointmentOutcomeDto {
    val creditTime = requireNotNull(courseCompletionResolution.creditTimeDetails) {
      "Missing credit time details"
    }
    val appointmentTimesPair = calculateAppointmentTimes(creditTime.minutesToCredit, courseCompletionEvent)
    return UpdateAppointmentOutcomeDto(
      deliusId = existingAppointment.id,
      deliusVersionToUpdate = existingAppointment.version,
      date = courseCompletionResolution.creditTimeDetails.date,
      startTime = appointmentTimesPair.first,
      endTime = appointmentTimesPair.second,
      contactOutcomeCode = creditTime.contactOutcomeCode,
      attendanceData = createAttendanceData(),
      supervisorOfficerCode = existingAppointment.supervisorOfficerCode,
      notes = buildNote(creditTime.notes, courseCompletionEvent),
      alertActive = creditTime.alertActive,
      sensitive = creditTime.sensitive,
    )
  }

  private fun buildNote(
    userNotes: String?,
    courseCompletionEvent: EteCourseCompletionEventEntity,
  ) = buildString {
    for (event in getAllAttemptsForCourseCompletionEvent(courseCompletionEvent)) {
      val completionDateLocal = event.completionDateTime.toLocalDateTimeEuropeLondon()

      append("'")
      append(event.courseName)
      append("' was completed on ")
      append(event.provider)
      append(" at ")
      append(completionDateLocal.toLocalTime().formatForUser())
      append(" on ")
      append(completionDateLocal.toLocalDate().formatForUser())
      append(" and resulted in a ")
      append(event.status.formatForUser())
      append(" on attempt ")
      appendLine(event.attempts)
    }

    if (userNotes?.isNotBlank() == true) {
      appendLine(userNotes)
    }
  }.trimEnd()

  private fun calculateAppointmentTimes(minutesToCredit: Long, courseCompletionEvent: EteCourseCompletionEventEntity): Pair<LocalTime, LocalTime> {
    val endTime = courseCompletionEvent.completionDateTime.toLocalTimeEuropeLondon()
    val creditLimit = ChronoUnit.MINUTES.between(LocalTime.MIDNIGHT, endTime)
    return if (minutesToCredit > creditLimit) {
      Pair(DEFAULT_APPOINTMENT_START_TIME, DEFAULT_APPOINTMENT_START_TIME.plusMinutes(minutesToCredit))
    } else {
      Pair(endTime.minusMinutes(minutesToCredit), endTime)
    }
  }

  fun createAttendanceData() = AttendanceDataDto(
    hiVisWorn = false,
    workedIntensively = false,
    penaltyTime = null,
    penaltyMinutes = null,
    workQuality = AppointmentWorkQualityDto.NOT_APPLICABLE,
    behaviour = AppointmentBehaviourDto.NOT_APPLICABLE,
  )

  fun toResolutionEntityForDontCreditTime(
    id: UUID,
    courseCompletionEvent: EteCourseCompletionEventEntity,
    courseCompletionResolution: CourseCompletionResolutionDto,
  ) = toBaselineResolutionEntity(
    id,
    courseCompletionEvent,
    courseCompletionResolution,
    EteCourseCompletionResolution.DONT_CREDIT_TIME,
  ).copy(
    notes = courseCompletionResolution.dontCreditTimeDetails!!.notes,
  )

  fun toResolutionEntityForCreditTime(
    id: UUID,
    courseCompletionEvent: EteCourseCompletionEventEntity,
    courseCompletionResolution: CourseCompletionResolutionDto,
    deliusAppointmentId: Long,
  ): EteCourseCompletionEventResolutionEntity {
    val creditTime = courseCompletionResolution.creditTimeDetails!!

    return toBaselineResolutionEntity(
      id,
      courseCompletionEvent,
      courseCompletionResolution,
      EteCourseCompletionResolution.CREDIT_TIME,
    ).copy(
      deliusEventNumber = creditTime.deliusEventNumber,
      deliusAppointmentId = deliusAppointmentId,
      deliusAppointmentCreated = creditTime.appointmentIdToUpdate == null,
      projectCode = creditTime.projectCode,
      minutesCredited = creditTime.minutesToCredit,
      contactOutcome = contactOutcomeEntityRepository.findByCode(creditTime.contactOutcomeCode),
      notes = creditTime.notes,
    )
  }

  private fun toBaselineResolutionEntity(
    id: UUID,
    courseCompletionEvent: EteCourseCompletionEventEntity,
    courseCompletionResolution: CourseCompletionResolutionDto,
    type: EteCourseCompletionResolution,
  ) = EteCourseCompletionEventResolutionEntity(
    id = id,
    eteCourseCompletionEvent = courseCompletionEvent,
    resolution = type,
    createdAt = OffsetDateTime.now(),
    createdByUsername = contextService.getUserName(),
    crn = courseCompletionResolution.crn,
  )

  fun toCourseCompletionEventEntity(message: EducationCourseCompletionMessage): EteCourseCompletionEventEntity {
    val messageAttributes = message.messageAttributes
    val pdu = messageAttributes.pdu
    return EteCourseCompletionEventEntity(
      id = UUID.randomUUID(),
      firstName = messageAttributes.firstName,
      lastName = messageAttributes.lastName,
      dateOfBirth = messageAttributes.dateOfBirth,
      region = messageAttributes.region,
      pdu = communityCampusPduEntityRepository.findByNameIgnoreCase(pdu.trim()) ?: error("Cannot find PDU for name $pdu"),
      office = messageAttributes.office,
      email = messageAttributes.email,
      courseName = messageAttributes.courseName,
      courseType = messageAttributes.courseType,
      provider = messageAttributes.provider,
      completionDateTime = messageAttributes.completionDateTime,
      status = EteCourseCompletionEventStatus.fromMessage(messageAttributes.status),
      totalTimeMinutes = messageAttributes.totalTimeMinutes,
      expectedTimeMinutes = messageAttributes.expectedTimeMinutes,
      externalReference = messageAttributes.externalReference,
      attempts = messageAttributes.attempts,
      receivedAt = OffsetDateTime.now(),
    )
  }

  private fun getAllAttemptsForCourseCompletionEvent(courseCompletionEvent: EteCourseCompletionEventEntity) = eteCourseCompletionEventEntityRepository.findAllWithFilters(
    providerCode = courseCompletionEvent.pdu.providerCode,
    pduId = null,
    officesCount = 0,
    offices = listOf(),
    resolutionStatus = ResolutionStatus.UNRESOLVED,
    courseFailures = CourseFailureFilter.SHOW_ALL,
    externalReference = courseCompletionEvent.externalReference,
    fromDate = null,
    toDate = null,
    availableFromDate = null,
    availableToDate = null,
    pageable = Pageable.unpaged(Sort.by(Sort.Order.asc("createdAt"))),
  ).toList()
}

fun EteCourseCompletionEventEntity.toDto() = EteCourseCompletionEventDto(
  id = id,
  firstName = firstName,
  lastName = lastName,
  dateOfBirth = dateOfBirth,
  region = region,
  pdu = pdu.toDto(),
  office = office,
  email = email,
  courseName = courseName,
  courseType = courseType,
  provider = provider,
  completionDateTime = completionDateTime,
  status = status.dtoType,
  totalTimeMinutes = totalTimeMinutes,
  expectedTimeMinutes = expectedTimeMinutes,
  attempts = attempts,
  externalReference = externalReference,
  importedOn = receivedAt.toLocalDateTime(),
  resolved = resolution != null,
)

fun EteCourseCompletionEventStatus.formatForUser(): String = when (this) {
  EteCourseCompletionEventStatus.PASSED -> "pass"
  EteCourseCompletionEventStatus.FAILED -> "fail"
}

fun EteCourseCompletionDraftResolutionEntity.toDto() = CourseCompletionDraftResolutionDto(
  crn = crn,
)
