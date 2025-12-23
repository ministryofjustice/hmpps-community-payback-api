package uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal

import java.time.Duration
import java.time.LocalTime
import java.time.temporal.ChronoUnit

fun shortestOf(duration1: Duration, duration2: Duration) = if (duration1 < duration2) {
  duration1
} else {
  duration2
}

fun minutesBetween(duration1: LocalTime, duration2: LocalTime): Duration = Duration.ofMinutes(ChronoUnit.MINUTES.between(duration1, duration2))
