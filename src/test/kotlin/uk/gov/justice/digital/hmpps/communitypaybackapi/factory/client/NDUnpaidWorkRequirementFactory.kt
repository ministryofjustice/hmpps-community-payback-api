package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDRequirementProgress
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDUnpaidWorkRequirement

fun NDUnpaidWorkRequirement.Companion.valid() = NDUnpaidWorkRequirement(
  requirementProgress = NDRequirementProgress.valid(),
  allocations = emptyList(),
  appointments = emptyList(),
)
