package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.scheduling

import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalDate
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingAllocations
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingExistingAppointments
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingNonWorkingDates
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingRequest
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingRequirementProgress
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingTrigger
import java.time.Duration

fun SchedulingRequest.Companion.empty() = SchedulingRequest(
  today = randomLocalDate(),
  trigger = SchedulingTrigger.AppointmentUpdate,
  requirement = SchedulingRequirementProgress(lengthMinutes = Duration.ZERO),
  allocations = SchedulingAllocations(emptyList()),
  existingAppointments = SchedulingExistingAppointments(emptyList()),
  nonWorkingDates = SchedulingNonWorkingDates(emptyList()),
)
