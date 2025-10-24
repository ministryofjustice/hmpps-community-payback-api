package uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers

import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.OffenderDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.OffenderInfoResult

fun OffenderInfoResult.toDto() = when (this) {
  is OffenderInfoResult.Full -> toDto()
  is OffenderInfoResult.Limited -> toDto()
  is OffenderInfoResult.NotFound -> toDto()
}

private fun OffenderInfoResult.Full.toDto() = OffenderDto.OffenderFullDto(
  crn = this.crn,
  forename = this.summary.name.forename,
  surname = this.summary.name.surname,
  middleNames = this.summary.name.middleNames,
  dateOfBirth = this.summary.dateOfBirth,
)

private fun OffenderInfoResult.Limited.toDto() = OffenderDto.OffenderLimitedDto(crn = this.crn)

private fun OffenderInfoResult.NotFound.toDto() = OffenderDto.OffenderNotFoundDto(crn = this.crn)
