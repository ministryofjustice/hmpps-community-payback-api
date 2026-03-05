package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.CommunityCampusPduEntity
import java.util.UUID

fun CommunityCampusPduEntity.Companion.valid() = CommunityCampusPduEntity(
  id = UUID.randomUUID(),
  name = String.random(20),
  providerCode = String.random(5),
)
