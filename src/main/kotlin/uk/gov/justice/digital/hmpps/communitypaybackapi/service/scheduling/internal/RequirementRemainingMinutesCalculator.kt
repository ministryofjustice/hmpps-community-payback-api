package uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.minutesBetween
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingExistingAppointments
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingRequirementProgress
import java.time.Duration
import java.time.LocalDate

object RequirementRemainingMinutesCalculator {

  fun calculateRemainingMinutesAsOfToday(
    today: LocalDate,
    requirement: SchedulingRequirementProgress,
    existingAppointments: SchedulingExistingAppointments,
  ): Duration {
    val lengthMinutes = requirement.lengthMinutes.toMinutes()

    val pastMinutesOffered =
      existingAppointments.appointments.filter { it.date.isBefore(today) }
        .filter { !it.hasOutcome }
        .sumOf { minutesBetween(it.startTime, it.endTime).toMinutes() }

    val allMinutesCredited = existingAppointments.appointments
      .filter { it.hasOutcome }
      .sumOf { it.timeCredited?.toMinutes() ?: 0 }

    return Duration.ofMinutes(lengthMinutes - pastMinutesOffered - allMinutesCredited)
  }
}
