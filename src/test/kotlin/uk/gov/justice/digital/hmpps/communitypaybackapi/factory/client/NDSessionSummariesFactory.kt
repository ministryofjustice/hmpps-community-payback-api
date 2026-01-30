package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSessionSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSessionSummary

fun NDSessionSummaries.Companion.valid() = NDSessionSummaries(
  listOf(
    NDSessionSummary.valid(),
    NDSessionSummary.valid(),
  ),
)
