package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import java.util.UUID

fun ContactOutcomeEntity.Companion.valid() = ContactOutcomeEntity(
  id = UUID.randomUUID(),
  code = String.random(length = 3),
  name = String.random(length = 20),
  enforceable = false,
  attended = false,
  groups = emptyList(),
)
