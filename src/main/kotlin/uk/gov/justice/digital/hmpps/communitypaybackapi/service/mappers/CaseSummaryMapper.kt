package uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.OffenderDto

fun CaseSummary.toDto() = if (isLimited()) {
  OffenderDto.OffenderLimitedDto(
    crn = crn,
  )
} else {
  OffenderDto.OffenderFullDto(
    crn = crn,
    forename = name.forename,
    surname = name.surname,
    middleNames = name.middleNames,
    dateOfBirth = dateOfBirth,
  )
}

private fun CaseSummary.isLimited() = this.currentExclusion || this.currentRestriction
