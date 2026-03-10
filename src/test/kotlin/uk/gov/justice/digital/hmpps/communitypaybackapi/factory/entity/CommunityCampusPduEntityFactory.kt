package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity

import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.CommunityCampusPduEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import java.util.UUID

fun CommunityCampusPduEntity.Companion.valid() = CommunityCampusPduEntity(
  id = UUID.randomUUID(),
  name = String.Companion.random(20),
  providerCode = String.random(5),
)
