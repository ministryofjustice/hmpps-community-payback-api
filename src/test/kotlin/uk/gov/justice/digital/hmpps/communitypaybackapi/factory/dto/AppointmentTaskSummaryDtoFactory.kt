package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto

import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentTaskSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.OffenderDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import java.time.LocalDate
import java.util.UUID

fun AppointmentTaskSummaryDto.Companion.valid() = AppointmentTaskSummaryDto(
  taskId = UUID.randomUUID(),
  appointment = AppointmentSummaryDto.valid(),
  offender = OffenderDto.validFull().copy(middleNames = null, dateOfBirth = null),
  date = LocalDate.now(),
  projectTypeName = String.random(50),
)
