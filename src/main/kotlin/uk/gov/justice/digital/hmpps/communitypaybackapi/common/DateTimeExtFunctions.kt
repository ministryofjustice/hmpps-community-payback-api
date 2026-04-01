package uk.gov.justice.digital.hmpps.communitypaybackapi.common

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

fun LocalDate.onOrAfter(other: LocalDate): Boolean = !this.isBefore(other)

fun LocalDate.onOrBefore(other: LocalDate): Boolean = !this.isAfter(other)

fun shortestOf(duration1: Duration, duration2: Duration) = if (duration1 < duration2) duration1 else duration2

fun minutesBetween(start: LocalTime, end: LocalTime): Duration = Duration.ofMinutes(ChronoUnit.MINUTES.between(start, end))

fun daysUntil(start: LocalDate, end: LocalDate): Long = ChronoUnit.DAYS.between(start, end)

fun LocalDate.findNextOrSameDateForDayOfWeek(dayOfWeek: DayOfWeek): LocalDate = with(TemporalAdjusters.nextOrSame(dayOfWeek))

fun LocalDate.atFirstSecondOfDay(): OffsetDateTime = this.atTime(0, 0).atZone(ZoneId.of("UTC")).toOffsetDateTime()

fun LocalDate.atLastSecondOfDay(): OffsetDateTime = this.atTime(23, 59, 59).atZone(ZoneId.of("UTC")).toOffsetDateTime()

fun LocalDate.formatForUser(): String = format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(Locale.UK))

fun DayOfWeek.formatForUser(): String = getDisplayName(TextStyle.FULL, Locale.UK)

fun Duration.formatForUser(): String = "${toHoursPart()} hours ${toMinutesPart()} minutes"
