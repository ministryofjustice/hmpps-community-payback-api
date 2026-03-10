package uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseDetail
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseDetailsSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CaseDetailsSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UnpaidWorkDetailsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.EteService.Companion.ETE_ALLOWANCE_OF_TOTAL_REQUIREMENT

fun NDCaseDetailsSummary.toDto() = CaseDetailsSummaryDto(
  unpaidWorkDetails = this.unpaidWorkDetails.map { it.toDto() },
)

fun NDCaseDetail.toDto(): UnpaidWorkDetailsDto {
  // calculation rounding (towards 0) matches NDelius behaviour
  val allowedEteMinutes = (requiredMinutes * ETE_ALLOWANCE_OF_TOTAL_REQUIREMENT).toLong()
  val completedEteMinutes = this.completedEteMinutes
  val remainingEteMinutes = allowedEteMinutes - completedEteMinutes

  return UnpaidWorkDetailsDto(
    eventNumber = this.eventNumber,
    sentenceDate = this.sentenceDate,
    requiredMinutes = this.requiredMinutes,
    completedMinutes = this.completedMinutes,
    adjustments = this.adjustments,
    allowedEteMinutes = allowedEteMinutes,
    completedEteMinutes = completedEteMinutes,
    remainingEteMinutes = remainingEteMinutes,
  )
}
