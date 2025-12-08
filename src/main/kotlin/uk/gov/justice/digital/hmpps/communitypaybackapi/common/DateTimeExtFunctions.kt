package uk.gov.justice.digital.hmpps.communitypaybackapi.common

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

fun LocalDate.onOrAfter(other: LocalDate): Boolean = !this.isBefore(other)

fun LocalDate.onOrBefore(other: LocalDate): Boolean = !this.isAfter(other)

fun shortestOf(duration1: Duration, duration2: Duration) = if (duration1 < duration2) duration1 else duration2

fun minutesBetween(start: LocalTime, end: LocalTime): Duration = Duration.ofMinutes(ChronoUnit.MINUTES.between(start, end))

fun daysUntil(start: LocalDate, end: LocalDate): Long = ChronoUnit.DAYS.between(start, end)

fun LocalDate.findNextOrSameDateForDayOfWeek(dayOfWeek: DayOfWeek): LocalDate = with(TemporalAdjusters.nextOrSame(dayOfWeek))
