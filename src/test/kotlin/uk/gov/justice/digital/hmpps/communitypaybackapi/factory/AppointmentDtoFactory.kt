package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.OffenderDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.PickUpDataDto
import java.util.UUID
import kotlin.Long
import kotlin.String

fun AppointmentDto.Companion.valid() = AppointmentDto(
  id = Long.random(),
  communityPaybackId = UUID.randomUUID(),
  version = UUID.randomUUID(),
  deliusEventNumber = Int.random(0, 50),
  projectName = String.random(),
  projectCode = String.random(),
  projectTypeName = String.random(),
  projectTypeCode = String.random(),
  offender = OffenderDto.OffenderLimitedDto(crn = String.random()),
  supervisingTeam = String.random(),
  supervisingTeamCode = String.random(),
  providerCode = String.random(),
  pickUpData = PickUpDataDto.valid(),
  date = randomLocalDate(),
  startTime = randomLocalTime(),
  endTime = randomLocalTime(),
  contactOutcomeCode = "OUTCOME1",
  attendanceData = null,
  enforcementData = null,
  supervisorOfficerCode = String.random(),
  notes = null,
  sensitive = null,
  alertActive = null,
)
