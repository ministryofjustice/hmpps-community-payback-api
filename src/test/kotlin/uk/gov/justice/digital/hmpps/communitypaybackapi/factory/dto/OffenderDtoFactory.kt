package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto

import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.OffenderDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalDate

fun OffenderDto.Companion.validFull() = OffenderDto.OffenderFullDto(
  crn = String.random(5),
  forename = String.random(50),
  surname = String.random(50),
  middleNames = emptyList(),
  dateOfBirth = randomLocalDate(),
)
