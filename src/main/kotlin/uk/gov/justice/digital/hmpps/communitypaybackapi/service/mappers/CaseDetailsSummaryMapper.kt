package uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseDetail
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseDetailsSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CaseDetailsSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UnpaidWorkDetailsDto

fun NDCaseDetailsSummary.toDto() = CaseDetailsSummaryDto(
  unpaidWorkDetails = this.unpaidWorkDetails.map { it.toDto() },
)

fun NDCaseDetail.toDto() = UnpaidWorkDetailsDto(
  eventNumber = this.eventNumber,
  sentenceDate = this.sentenceDate,
  requiredMinutes = this.requiredMinutes,
  completedMinutes = this.completedMinutes,
  adjustments = this.adjustments,
  completedEteMinutes = this.completedEteMinutes,
)
