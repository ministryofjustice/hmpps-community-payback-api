package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectSessionSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalDate
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalTime

fun ProjectSessionSummary.Companion.valid() = ProjectSessionSummary(
  projectName = String.random(20),
  date = randomLocalDate(),
  startTime = randomLocalTime(),
  endTime = randomLocalTime(),
  projectCode = String.random(5),
  allocatedCount = Int.random(),
  compliedOutcomeCount = Int.random(),
  enforcementActionNeededCount = Int.random(),
)
