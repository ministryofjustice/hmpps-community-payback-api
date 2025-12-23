package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.scheduling

import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingRequirementProgress
import java.time.Duration

fun SchedulingRequirementProgress.Companion.valid() = SchedulingRequirementProgress(lengthMinutes = Duration.ZERO)
