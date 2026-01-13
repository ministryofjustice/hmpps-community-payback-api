package uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.minutesBetween
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingExistingAppointments
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingRequirement
import java.time.Duration
import java.time.LocalDate

object RequirementRemainingMinutesCalculator {

  fun calculateRemainingMinutesAsOfToday(
    today: LocalDate,
    requirement: SchedulingRequirement,
    existingAppointments: SchedulingExistingAppointments,
  ): Duration {
    val lengthMinutes = requirement.requirementLengthMinutes.toMinutes()

    val pastMinutesOffered =
      existingAppointments.appointments.filter { it.date.isBefore(today) }
        .filter { !it.hasOutcome }
        .sumOf { minutesBetween(it.startTime, it.endTime).toMinutes() }

    val allMinutesCredited = existingAppointments.appointments
      .filter { it.hasOutcome }
      .sumOf { it.minutesCredited?.toMinutes() ?: 0 }

    return Duration.ofMinutes(lengthMinutes - pastMinutesOffered - allMinutesCredited)
  }
}
