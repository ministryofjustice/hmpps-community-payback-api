package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CaseName
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random

fun CaseSummary.Companion.valid() = CaseSummary(
  crn = String.Companion.random(8),
  name = CaseName(
    forename = String.random(10),
    surname = String.random(10),
    middleNames = emptyList(),
  ),
)
