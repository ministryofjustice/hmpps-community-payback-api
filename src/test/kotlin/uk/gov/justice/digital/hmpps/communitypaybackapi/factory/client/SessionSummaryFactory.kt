package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.SessionSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalDate

fun SessionSummary.Companion.valid() = SessionSummary(
  project = ProjectSummary.valid(),
  date = randomLocalDate(),
  allocatedCount = Int.random(),
  outcomeCount = Int.random(),
  enforcementActionCount = Int.random(),
)
