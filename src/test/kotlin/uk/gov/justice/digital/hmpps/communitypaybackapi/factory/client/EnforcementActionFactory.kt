package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.EnforcementAction
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalDate

fun EnforcementAction.Companion.valid() = EnforcementAction(
  code = String.random(5),
  description = String.random(20),
  respondBy = randomLocalDate(),
)
