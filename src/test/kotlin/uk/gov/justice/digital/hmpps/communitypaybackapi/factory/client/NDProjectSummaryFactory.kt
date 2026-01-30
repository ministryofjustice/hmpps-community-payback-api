package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProjectSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random

fun NDProjectSummary.Companion.valid() = NDProjectSummary(
  code = String.random(5),
  description = String.random(50),
)
