package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto

import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CaseDetailsSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UnpaidWorkDetailsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalDate
import kotlin.Long

fun CaseDetailsSummaryDto.Companion.valid() = CaseDetailsSummaryDto(
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
)
