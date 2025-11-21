package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.HourMinuteDuration
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.random.Random

fun Boolean.Companion.random() = Random.nextBoolean()
fun Int.Companion.random(from: Int = 0, to: Int = Int.MAX_VALUE) = Random.nextInt(from, to)
fun Long.Companion.random(from: Long = 0, to: Long = Long.MAX_VALUE) = Random.nextLong(from, to)
fun String.Companion.random(length: Int = 50) = String(CharArray(length) { (('A'..'Z') + ('a'..'z') + ('0'..'9')).random() })

fun randomLocalDate(): LocalDate = LocalDate.now().plusDays(Long.random(0, 2000))
fun randomLocalTime(): LocalTime = LocalTime.ofSecondOfDay(Long.random(0, 60 * 60 * 12))
fun randomOffsetDateTime(): OffsetDateTime = OffsetDateTime.of(randomLocalDate(), randomLocalTime(), ZoneOffset.UTC)
fun randomHourMinuteDuration(): HourMinuteDuration = HourMinuteDuration(Duration.ofHours(Long.random(0, 12)).plusMinutes(Long.random(0, 60)))
