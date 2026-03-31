package uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentBehaviourDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentWorkQualityDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AttendanceDataDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionResolutionDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.EteCourseCompletionEventDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.CommunityCampusPduEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntity
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
) {

  companion object {
    val APPOINTMENT_START_TIME: LocalTime = LocalTime.of(0, 0)
  }

  fun toCreateAppointmentDto(
    courseCompletionResolution: CourseCompletionResolutionDto,
  ): CreateAppointmentDto {
    val creditTime = courseCompletionResolution.creditTimeDetails!!
    return CreateAppointmentDto(
      id = AppointmentEntity.generateId(),
      crn = courseCompletionResolution.crn!!,
      deliusEventNumber = creditTime.deliusEventNumber,
      allocationId = null,
      projectCode = creditTime.projectCode,
      date = creditTime.date,
      startTime = APPOINTMENT_START_TIME,
      endTime = calculateEndTime(creditTime.minutesToCredit),
      pickUpLocationCode = null,
      pickUpLocationDescription = null,
      pickUpTime = null,
      contactOutcomeCode = creditTime.contactOutcomeCode,
      attendanceData = createAttendanceData(),
      supervisorOfficerCode = null,
      notes = creditTime.notes,
      alertActive = creditTime.alertActive,
      sensitive = creditTime.sensitive,
    )
  }

  fun toUpdateAppointmentDto(
    courseCompletionResolution: CourseCompletionResolutionDto,
    existingAppointment: AppointmentDto,
  ): UpdateAppointmentOutcomeDto {
    val creditTime = requireNotNull(courseCompletionResolution.creditTimeDetails) {
      "Missing credit time details"
    }

    if (existingAppointment.date != creditTime.date) {
      error("Changing an existing appointment's date is not currently supported")
    }

    return UpdateAppointmentOutcomeDto(
      deliusId = existingAppointment.id,
      deliusVersionToUpdate = existingAppointment.version,
      startTime = APPOINTMENT_START_TIME,
      endTime = calculateEndTime(creditTime.minutesToCredit),
      contactOutcomeCode = creditTime.contactOutcomeCode,
      attendanceData = createAttendanceData(),
      enforcementData = null,
      supervisorOfficerCode = existingAppointment.supervisorOfficerCode,
      notes = creditTime.notes,
      alertActive = creditTime.alertActive,
      sensitive = creditTime.sensitive,
    )
  }

  private fun calculateEndTime(
    minutesToCredit: Long,
  ): LocalTime {
    val creditLimit = ChronoUnit.MINUTES.between(APPOINTMENT_START_TIME, LocalTime.MIDNIGHT.minusMinutes(1))
    if (minutesToCredit > creditLimit) {
      error("Cannot credit more than $creditLimit minutes")
    }

    return APPOINTMENT_START_TIME.plusMinutes(minutesToCredit)
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
