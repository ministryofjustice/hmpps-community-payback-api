package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.RequirementProgress

fun RequirementProgress.Companion.valid() = RequirementProgress(
  requirementMinutes = Int.random(80, 1000),
  completedMinutes = Int.random(0, 1000),
)
