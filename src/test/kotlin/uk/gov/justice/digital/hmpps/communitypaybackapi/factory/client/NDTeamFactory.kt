package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDTeam
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random

fun NDTeam.Companion.valid() = NDTeam(
  code = String.random(),
  name = String.random(),
)
