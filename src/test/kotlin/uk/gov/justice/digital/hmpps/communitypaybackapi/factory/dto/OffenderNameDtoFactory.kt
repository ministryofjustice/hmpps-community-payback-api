package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto

import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.OffenderNameDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random

fun OffenderNameDto.Companion.valid() = OffenderNameDto(
  forename = String.random(50),
  surname = String.random(50),
)
