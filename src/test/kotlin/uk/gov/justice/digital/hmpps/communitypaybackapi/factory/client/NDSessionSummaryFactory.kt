package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProjectSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSessionSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalDate

fun NDSessionSummary.Companion.valid() = NDSessionSummary(
  project = NDProjectSummary.valid(),
  date = randomLocalDate(),
  allocatedCount = Int.random(),
  outcomeCount = Int.random(),
  enforcementActionCount = Int.random(),
)
