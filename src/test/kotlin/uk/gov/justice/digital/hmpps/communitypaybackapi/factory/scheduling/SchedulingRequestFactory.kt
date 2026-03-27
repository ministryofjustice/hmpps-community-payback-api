package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.scheduling

import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalDate
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal.SchedulingAllocations
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal.SchedulingExistingAppointments
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal.SchedulingNonWorkingDates
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal.SchedulingRequest
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal.SchedulingRequirement
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal.SchedulingTrigger

fun SchedulingRequest.Companion.valid() = SchedulingRequest(
  today = randomLocalDate(),
  trigger = SchedulingTrigger.valid(),
  requirement = SchedulingRequirement.valid(),
  allocations = SchedulingAllocations(emptyList()),
  existingAppointments = SchedulingExistingAppointments(emptyList()),
  nonWorkingDates = SchedulingNonWorkingDates(emptyList()),
  dryRun = false,
)
