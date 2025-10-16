package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.Team

fun Team.Companion.valid() = Team(
  code = String.random(),
  name = String.random(),
)
