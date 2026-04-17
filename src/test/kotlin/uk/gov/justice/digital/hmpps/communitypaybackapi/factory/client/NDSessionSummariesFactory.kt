package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSessionSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSessionSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.PageResponse

fun NDSessionSummaries.Companion.valid(): NDSessionSummaries {
  val summaries = listOf(
    NDSessionSummary.valid(),
    NDSessionSummary.valid(),
  )

  return NDSessionSummaries(
    sessions = summaries,
    pageResponse = PageResponse(
      content = summaries,
      page = PageResponse.PageMeta(50, 0, 2, 1),
    ),
  )
}
