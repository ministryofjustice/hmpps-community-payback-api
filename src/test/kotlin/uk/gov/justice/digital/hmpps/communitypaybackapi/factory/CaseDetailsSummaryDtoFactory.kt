package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CaseDetailsSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UnpaidWorkDetailsDto
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
  completedEteMinutes = Long.random(),
)
