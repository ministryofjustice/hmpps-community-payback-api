package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDNameCode
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random

fun NDNameCode.Companion.valid() = NDNameCode(
  name = String.random(20),
  code = String.random(5),
)
