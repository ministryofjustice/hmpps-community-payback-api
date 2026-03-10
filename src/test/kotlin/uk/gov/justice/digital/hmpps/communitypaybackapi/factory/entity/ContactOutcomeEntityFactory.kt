package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity

import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import java.util.UUID

fun ContactOutcomeEntity.Companion.valid() = ContactOutcomeEntity(
  id = UUID.randomUUID(),
  code = String.Companion.random(length = 3),
  name = String.random(length = 20),
  enforceable = false,
  attended = false,
  groups = emptyList(),
)
