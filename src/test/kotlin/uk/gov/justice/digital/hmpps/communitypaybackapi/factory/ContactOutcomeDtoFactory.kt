package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ContactOutcomeDto
import java.util.UUID

fun ContactOutcomeDto.Companion.valid() = ContactOutcomeDto(
  id = UUID.randomUUID(),
  name = String.random(),
  code = String.random(5),
  enforceable = Boolean.random(),
)
