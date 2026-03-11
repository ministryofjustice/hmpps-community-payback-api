package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity

import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EnforcementActionEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import java.util.UUID

fun EnforcementActionEntity.Companion.valid() = EnforcementActionEntity(
  id = UUID.randomUUID(),
  code = String.Companion.random(length = 3),
  name = String.random(length = 20),
  respondByDateRequired = false,
)
