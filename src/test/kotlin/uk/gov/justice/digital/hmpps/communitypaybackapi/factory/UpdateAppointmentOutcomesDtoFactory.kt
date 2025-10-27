package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import org.springframework.context.ApplicationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentBehaviourDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentWorkQualityDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AttendanceDataDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.EnforcementDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EnforcementActionEntityRepository
import java.util.UUID

fun UpdateAppointmentOutcomeDto.Companion.valid(
  contactOutcomeId: UUID = UUID.randomUUID(),
  enforcementActionId: UUID = UUID.randomUUID(),
) = UpdateAppointmentOutcomeDto(
  deliusId = Long.random(),
  deliusVersionToUpdate = UUID.randomUUID(),
  startTime = randomLocalTime(),
  endTime = randomLocalTime(),
  contactOutcomeId = contactOutcomeId,
  supervisorOfficerCode = String.random(),
  notes = String.random(400),
  attendanceData = AttendanceDataDto(
    hiVisWorn = Boolean.random(),
    workedIntensively = Boolean.random(),
    penaltyTime = randomLocalTime(),
    workQuality = AppointmentWorkQualityDto.entries.toTypedArray().random(),
    behaviour = AppointmentBehaviourDto.entries.toTypedArray().random(),
  ),
  enforcementData = EnforcementDto(
    enforcementActionId = enforcementActionId,
    respondBy = randomLocalDate(),
  ),
  formKeyToDelete = null,
  alertActive = Boolean.random(),
  sensitive = Boolean.random(),
)

fun UpdateAppointmentOutcomeDto.Companion.valid(ctx: ApplicationContext) = UpdateAppointmentOutcomeDto.valid(
  contactOutcomeId = ctx.getBean(ContactOutcomeEntityRepository::class.java).findAll().first().id,
  enforcementActionId = ctx.getBean(EnforcementActionEntityRepository::class.java).findAll().first().id,
)
