package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.scheduling

import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalDate
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalTime
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal.SchedulingAllocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal.SchedulingProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal.SchedulingRequiredAppointment

fun SchedulingRequiredAppointment.Companion.valid() = SchedulingRequiredAppointment(
  date = randomLocalDate(),
  startTime = randomLocalTime(),
  endTime = randomLocalTime(),
  project = SchedulingProject.valid(),
  allocation = SchedulingAllocation.valid(),
)
