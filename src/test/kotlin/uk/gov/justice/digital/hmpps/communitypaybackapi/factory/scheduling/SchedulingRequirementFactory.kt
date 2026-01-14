package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.scheduling

import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingRequirement
import java.time.Duration

fun SchedulingRequirement.Companion.valid() = SchedulingRequirement(
  crn = String.random(5),
  deliusEventNumber = Int.random(50),
  requirementLengthMinutes = Duration.ZERO,
)
