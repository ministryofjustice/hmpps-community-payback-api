package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDRequirementProgress
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random

fun NDRequirementProgress.Companion.valid() = NDRequirementProgress(
  requiredMinutes = Int.Companion.random(80, 1000),
  completedMinutes = Int.random(0, 1000),
  adjustments = Int.random(0, 100),
)
