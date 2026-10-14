package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity

import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import java.util.UUID

fun AdjustmentReasonEntity.Companion.valid() = AdjustmentReasonEntity(
  id = UUID.randomUUID(),
  deliusCode = String.random(5),
  name = String.random(50),
  displayName = String.random(length = 20),
  hintText = String.random(length = 50),
  maxMinutesAllowed = Int.random(1000),
  needsLinkToAppointment = Boolean.random(),
  rank = 0,
)
