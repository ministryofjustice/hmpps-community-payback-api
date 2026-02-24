package uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseDetail
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseDetailsSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CaseDetailDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CaseDetailsSummaryDto

fun NDCaseDetailsSummary.toDto() = CaseDetailsSummaryDto(
  unpaidWorkDetails = this.unpaidWorkDetails.map { it.toDto() },
)

fun NDCaseDetail.toDto() = CaseDetailDto(
  eventNumber = this.eventNumber,
  requiredMinutes = this.requiredMinutes,
  completedMinutes = this.completedMinutes,
  adjustments = this.adjustments,
  completedEteMinutes = this.completedEteMinutes,
)
