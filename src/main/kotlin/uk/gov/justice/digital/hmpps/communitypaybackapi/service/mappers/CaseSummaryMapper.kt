package uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.OffenderDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.OffenderNameDto

fun NDCaseSummary.toDto() = if (isLimited()) {
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

fun NDCaseSummary.toOffenderNameDto() = OffenderNameDto(this.name.forename, this.name.surname)

private fun NDCaseSummary.isLimited() = this.currentExclusion || this.currentRestriction
