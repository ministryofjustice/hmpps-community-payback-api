package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.scheduling

import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalDate
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalTime
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingAllocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingFrequency
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingProject
import java.time.DayOfWeek

fun SchedulingAllocation.Companion.valid() = SchedulingAllocation(
  id = Long.random(),
  alias = String.random(),
  project = SchedulingProject.valid(),
  frequency = SchedulingFrequency.WEEKLY,
  dayOfWeek = DayOfWeek.entries.toTypedArray().random(),
  startDateInclusive = randomLocalDate(),
  endDateInclusive = randomLocalDate(),
  startTime = randomLocalTime(),
  endTime = randomLocalTime(),
)
