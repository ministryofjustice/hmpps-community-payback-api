package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Team
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random

fun Team.Companion.valid() = Team(
  code = String.Companion.random(),
  name = String.random(),
)
