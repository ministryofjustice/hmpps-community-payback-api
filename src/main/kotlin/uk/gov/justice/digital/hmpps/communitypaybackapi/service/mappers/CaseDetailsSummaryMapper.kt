package uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseDetail
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseDetailsSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCodeDescription
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDMainOffence
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CaseDetailsSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourtDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.MainOffenceDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UnpaidWorkDetailsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.EteService.Companion.ETE_ALLOWANCE_OF_TOTAL_REQUIREMENT

fun NDCaseDetailsSummary.toDto() = CaseDetailsSummaryDto(
  offender = this.case.toDto(),
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
    eventOutcome = this.eventOutcome,
    upwStatus = this.upwStatus,
    referralDate = this.referralDate,
    convictionDate = this.convictionDate,
    court = this.court.toDto(),
    mainOffence = this.mainOffence.toDto(),
  )
}

fun NDCodeDescription.toDto() = CourtDto(
  code = this.code,
  description = this.description,
)

fun NDMainOffence.toDto() = MainOffenceDto(
  date = this.date,
  count = this.count,
  code = this.code,
  description = this.description,
)
