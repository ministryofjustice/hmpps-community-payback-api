package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.scheduling

import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalDate
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingAllocations
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingExistingAppointments
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingNonWorkingDates
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingRequest
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingRequirement

fun SchedulingRequest.Companion.empty() = SchedulingRequest(
  today = randomLocalDate(),
  trigger = String.random(10),
  requirement = SchedulingRequirement.valid(),
  allocations = SchedulingAllocations(emptyList()),
  existingAppointments = SchedulingExistingAppointments(emptyList()),
  nonWorkingDates = SchedulingNonWorkingDates(emptyList()),
  dryRun = false,
)
