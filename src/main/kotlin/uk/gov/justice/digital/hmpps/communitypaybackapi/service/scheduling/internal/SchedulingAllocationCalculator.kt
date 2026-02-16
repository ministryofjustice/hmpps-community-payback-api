package uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.daysUntil
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.findNextOrSameDateForDayOfWeek
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.onOrBefore
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingAllocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingAllocations
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingFrequency
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingRequiredAppointment
import java.time.LocalDate

/**
 * @return the next appointment date for this allocation that occurs on or after the given date, or null if one doesn't exist
 */
fun SchedulingAllocation.nextPotentialAppointmentDateOnOrAfter(
  onOrAfter: LocalDate,
  alreadyScheduledAppointments: List<SchedulingRequiredAppointment>,
): LocalDate? {
  val nextDate = when (frequency) {
    SchedulingFrequency.ONCE -> calculateOnceFrequency(onOrAfter, alreadyScheduledAppointments)
    SchedulingFrequency.WEEKLY -> calculateWeeklyFrequency(onOrAfter)
    SchedulingFrequency.FORTNIGHTLY -> calculateFortnightlyFrequency(onOrAfter)
  }

  return nextDate.takeIf { isWithinAllocationPeriod(it) }
}

private fun SchedulingAllocation.isWithinAllocationPeriod(date: LocalDate?) = date != null && (endDateInclusive == null || date.onOrBefore(endDateInclusive))

fun SchedulingAllocations.anyPotentialAppointmentsOnOrAfter(
  onOrAfter: LocalDate,
  alreadyScheduledAppointments: List<SchedulingRequiredAppointment>,
) = allocations.any { it.nextPotentialAppointmentDateOnOrAfter(onOrAfter, alreadyScheduledAppointments) != null }

fun SchedulingAllocations.anyPotentialAppointmentsOn(
  on: LocalDate,
  alreadyScheduledAppointments: List<SchedulingRequiredAppointment>,
) = allocations.any { it.nextPotentialAppointmentDateOnOrAfter(on, alreadyScheduledAppointments) == on }

private fun SchedulingAllocation.calculateOnceFrequency(
  onOrAfter: LocalDate,
  alreadyScheduledAppointments: List<SchedulingRequiredAppointment>,
) = if (alreadyScheduledAppointments.any { it.allocation == this }) {
  null
} else {
  earliestPotentialAppointmentDateOnOrAfter(onOrAfter).findNextOrSameDateForDayOfWeek(dayOfWeek)
}

private fun SchedulingAllocation.calculateWeeklyFrequency(
  onOrAfter: LocalDate,
) = earliestPotentialAppointmentDateOnOrAfter(onOrAfter).findNextOrSameDateForDayOfWeek(dayOfWeek)

private fun SchedulingAllocation.calculateFortnightlyFrequency(
  onOrAfter: LocalDate,
): LocalDate {
  val daysSinceAllocationStartToEarliestPossibleDate = daysUntil(startDateInclusive, earliestPotentialAppointmentDateOnOrAfter(onOrAfter))
  val fortnightsSinceAllocationStartToEarliestPossibleDate = daysSinceAllocationStartToEarliestPossibleDate.floorDiv(14)
  val earliestPossibleDateWithinFortnightCadence = startDateInclusive.plusWeeks(2 * fortnightsSinceAllocationStartToEarliestPossibleDate)

  return earliestPossibleDateWithinFortnightCadence.findNextOrSameDateForDayOfWeek(dayOfWeek)
}

private fun SchedulingAllocation.earliestPotentialAppointmentDateOnOrAfter(onOrAfter: LocalDate): LocalDate = maxOf(onOrAfter, startDateInclusive)
