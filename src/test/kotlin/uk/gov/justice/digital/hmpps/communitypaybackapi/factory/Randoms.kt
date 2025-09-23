package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import java.time.LocalDate
import java.time.LocalTime
import kotlin.random.Random

fun Boolean.Companion.random() = Random.nextBoolean()
fun Int.Companion.random(from: Int = 0, to: Int = Int.MAX_VALUE) = Random.nextInt(from, to)
fun Long.Companion.random(from: Long = 0, to: Long = Long.MAX_VALUE) = Random.nextLong(from, to)
fun String.Companion.random(length: Int = 50) = String(CharArray(length) { (('A'..'Z') + ('a'..'z') + ('0'..'9')).random() })

fun randomLocalDate(): LocalDate = LocalDate.now().plusDays(Long.random(0, 2000))
fun randomLocalTime(): LocalTime = LocalTime.ofSecondOfDay(Long.random(0, 60 * 60 * 12))
