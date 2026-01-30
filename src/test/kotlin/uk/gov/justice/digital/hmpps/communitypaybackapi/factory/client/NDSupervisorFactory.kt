package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSupervisor
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random

fun NDSupervisor.Companion.valid() = NDSupervisor(
  code = String.random(5),
  isUnpaidWorkTeamMember = Boolean.random(),
  unpaidWorkTeams = emptyList(),
)
