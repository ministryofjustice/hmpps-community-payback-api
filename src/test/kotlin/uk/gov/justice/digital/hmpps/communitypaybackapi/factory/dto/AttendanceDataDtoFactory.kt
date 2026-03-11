package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto

import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentBehaviourDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentWorkQualityDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AttendanceDataDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import kotlin.collections.random

fun AttendanceDataDto.Companion.valid() = AttendanceDataDto(
  hiVisWorn = Boolean.Companion.random(),
  workedIntensively = Boolean.random(),
  penaltyTime = null,
  workQuality = AppointmentWorkQualityDto.entries.toTypedArray().random(),
  behaviour = AppointmentBehaviourDto.entries.toTypedArray().random(),
)
