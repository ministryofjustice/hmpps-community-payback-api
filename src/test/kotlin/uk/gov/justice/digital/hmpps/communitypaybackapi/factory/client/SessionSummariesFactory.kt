package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.SessionSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.SessionSummary

fun SessionSummaries.Companion.valid() = SessionSummaries(
  listOf(
    SessionSummary.valid(),
    SessionSummary.valid(),
  ),
)
