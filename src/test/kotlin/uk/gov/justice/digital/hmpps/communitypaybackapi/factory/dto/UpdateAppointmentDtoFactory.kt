package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto

import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AttendanceDataDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

fun UpdateAppointmentsDto.Companion.valid() = UpdateAppointmentsDto(
  updates = listOf(UpdateAppointmentDto.valid()),
)

fun UpdateAppointmentDto.Companion.valid() = UpdateAppointmentDto(
  deliusId = Long.random(),
  deliusVersionToUpdate = UUID.randomUUID(),
  date = LocalDate.now(),
  startTime = LocalTime.of(10, 0),
  endTime = LocalTime.of(16, 0),
  contactOutcomeCode = String.random(5),
  supervisorOfficerCode = String.random(),
  notes = String.random(400),
  attendanceData = AttendanceDataDto.valid(),
  alertActive = Boolean.random(),
  sensitive = false,
)

fun UpdateAppointmentDto.Companion.valid(ctx: ApplicationContext) = UpdateAppointmentDto.valid().copy(
  contactOutcomeCode = ctx.getBean<ContactOutcomeEntityRepository>().findAll().first().code,
)
