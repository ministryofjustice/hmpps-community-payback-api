package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EnforcementActionEntity
import java.util.UUID

fun EnforcementActionEntity.Companion.valid() = EnforcementActionEntity(
  id = UUID.randomUUID(),
  code = String.random(length = 3),
  name = String.random(length = 20),
  respondByDateRequired = false,
)
