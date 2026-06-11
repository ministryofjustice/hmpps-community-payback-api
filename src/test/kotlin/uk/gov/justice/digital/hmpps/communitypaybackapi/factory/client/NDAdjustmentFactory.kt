package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAdjustment
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAdjustmentType
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDNameCode
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomPastLocalDate
import java.util.UUID

fun NDAdjustment.Companion.valid() = NDAdjustment(
  id = Long.random(),
  reference = UUID.randomUUID(),
  type = NDAdjustmentType.entries.random(),
  date = randomPastLocalDate(),
  reason = NDNameCode.valid(),
  minutes = Int.random(0, 60),
)
