package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Supervisor
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random

fun Supervisor.Companion.valid() = Supervisor(
  code = String.random(5),
  isUnpaidWorkTeamMember = Boolean.random(),
)
