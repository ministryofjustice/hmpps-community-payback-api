package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectSessionSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalDate
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalTime

fun ProjectSessionSummary.Companion.valid() = ProjectSessionSummary(
  project = ProjectSummary.valid(),
  date = randomLocalDate(),
  startTime = randomLocalTime(),
  endTime = randomLocalTime(),
  allocatedCount = Int.random(),
  compliedOutcomeCount = Int.random(),
  enforcementActionNeededCount = Int.random(),
)
