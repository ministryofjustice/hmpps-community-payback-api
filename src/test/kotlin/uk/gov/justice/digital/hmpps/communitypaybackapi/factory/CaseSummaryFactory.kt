package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CaseName
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CaseSummary

fun CaseSummary.Companion.valid() = CaseSummary(
  crn = String.random(8),
  name = CaseName(
    forename = String.random(10),
    surname = String.random(10),
    middleNames = emptyList(),
  ),
)
