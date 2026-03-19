package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto

import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CaseDetailsSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourtDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.MainOffenceDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.OffenderDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UnpaidWorkDetailsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalDate
import kotlin.Long

fun CaseDetailsSummaryDto.Companion.valid() = CaseDetailsSummaryDto(
  offender = OffenderDto.validFull(),
  unpaidWorkDetails = listOf(
    UnpaidWorkDetailsDto.valid(),
  ),
)

fun UnpaidWorkDetailsDto.Companion.valid() = UnpaidWorkDetailsDto(
  eventNumber = Long.random(),
  sentenceDate = randomLocalDate(),
  requiredMinutes = Long.random(),
  completedMinutes = Long.random(),
  adjustments = Long.random(),
  allowedEteMinutes = Long.random(),
  completedEteMinutes = Long.random(),
  remainingEteMinutes = Long.random(),
  eventOutcome = String.random(),
  upwStatus = String.random(),
  referralDate = randomLocalDate(),
  convictionDate = randomLocalDate(),
  court = CourtDto.valid(),
  mainOffence = MainOffenceDto.valid(),
)

fun CourtDto.Companion.valid() = CourtDto(
  code = String.random(),
  description = String.random(),
)

fun MainOffenceDto.Companion.valid() = MainOffenceDto(
  code = String.random(),
  description = String.random(),
  date = randomLocalDate(),
  count = Int.random(),
)
