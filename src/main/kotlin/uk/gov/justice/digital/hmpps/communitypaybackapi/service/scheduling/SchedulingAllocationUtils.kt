package uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * @return null if there is usable date after the given [onOrAfter] date
 */
@SuppressWarnings("MagicNumber")
fun SchedulingAllocation.nextAppointmentOnOrAfter(
  scheduledAppointments: List<SchedulingRequiredAppointment>,
  onOrAfter: LocalDate,
): LocalDate? {
  val nextDate = when (frequency) {
    SchedulingFrequency.ONCE -> {
      // note - this approach emulates a bug in NDelius because it 'resets' the once allocation to apply from whenever
      // the scheduling is run, ignoring past appointments. It should be possible to instead calculate when the
      // 'first' (and only) once appointment would be scheduled and use that to decide if one is available
      if (scheduledAppointments.any { it.allocation == this }) {
        null
      } else {
        earliestPossibleDate(onOrAfter).with(TemporalAdjusters.nextOrSame(dayOfWeek))
      }
    }

    SchedulingFrequency.WEEKLY -> {
      earliestPossibleDate(onOrAfter).with(TemporalAdjusters.nextOrSame(dayOfWeek))
    }

    SchedulingFrequency.FORTNIGHTLY -> {
      val completeFortnightsSinceAllocationStarted =
        daysBetween(startDateInclusive, earliestPossibleDate(onOrAfter)).floorDiv(14)
      val earliestPossibleDateWithinFortnightCadence =
        startDateInclusive.plusWeeks(2 * completeFortnightsSinceAllocationStarted)

      earliestPossibleDateWithinFortnightCadence.with(TemporalAdjusters.nextOrSame(dayOfWeek))
    }
  }

  return if (endDateInclusive == null) {
    nextDate
  } else if (nextDate != null && (nextDate == endDateInclusive || nextDate.isBefore(endDateInclusive))) {
    nextDate
  } else {
    null
  }
}

private fun SchedulingAllocation.earliestPossibleDate(onOrAfter: LocalDate) = if (startDateInclusive.isAfter(onOrAfter)) {
  startDateInclusive
} else {
  onOrAfter
}

private fun daysBetween(duration1: LocalDate, duration2: LocalDate): Long = ChronoUnit.DAYS.between(duration1, duration2)
