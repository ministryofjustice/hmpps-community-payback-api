package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.RequirementProgress
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random

fun RequirementProgress.Companion.valid() = RequirementProgress(
  requirementMinutes = Int.Companion.random(80, 1000),
  completedMinutes = Int.random(0, 1000),
)
