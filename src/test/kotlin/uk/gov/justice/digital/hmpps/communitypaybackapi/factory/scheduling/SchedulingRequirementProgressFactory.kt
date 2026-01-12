package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.scheduling

import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingRequirement
import java.time.Duration

fun SchedulingRequirement.Companion.valid() = SchedulingRequirement(requirementLengthMinutes = Duration.ZERO)
