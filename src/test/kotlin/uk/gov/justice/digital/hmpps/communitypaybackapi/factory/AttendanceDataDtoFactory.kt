package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentBehaviourDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentWorkQualityDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AttendanceDataDto
import kotlin.collections.random

fun AttendanceDataDto.Companion.valid() = AttendanceDataDto(
  hiVisWorn = Boolean.random(),
  workedIntensively = Boolean.random(),
  penaltyTime = randomLocalTime(),
  workQuality = AppointmentWorkQualityDto.entries.toTypedArray().random(),
  behaviour = AppointmentBehaviourDto.entries.toTypedArray().random(),
)
