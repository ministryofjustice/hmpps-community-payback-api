package uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers

import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CaseDetailsSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CommonCaseDetailsSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CommonUnpaidWorkDetailsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UnpaidWorkDetailsDto

fun CaseDetailsSummaryDto.toCommonDto() = CommonCaseDetailsSummaryDto(
  offender = offender,
  unpaidWorkDetails = unpaidWorkDetails.map { it.toCommonDto() },
)

fun UnpaidWorkDetailsDto.toCommonDto(): CommonUnpaidWorkDetailsDto {
  val totalCompletedMinutes = completedMinutes - adjustments
  return CommonUnpaidWorkDetailsDto(
    eventNumber = eventNumber,
    sentenceDate = sentenceDate,
    requiredMinutes = requiredMinutes,
    attendedAppointmentsMinutes = completedMinutes,
    totalAdjustmentMinutes = adjustments,
    completedMinutes = totalCompletedMinutes,
    remainingMinutes = requiredMinutes - totalCompletedMinutes,
    allowedEteMinutes = allowedEteMinutes,
    completedEteMinutes = completedEteMinutes,
    remainingEteMinutes = remainingEteMinutes,
    eventOutcome = eventOutcome,
    upwStatus = upwStatus,
    referralDate = referralDate,
    convictionDate = convictionDate,
    court = court,
    mainOffence = mainOffence,
  )
}
