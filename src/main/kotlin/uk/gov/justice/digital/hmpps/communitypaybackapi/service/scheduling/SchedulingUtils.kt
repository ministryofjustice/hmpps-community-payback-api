package uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling

import java.time.Duration
import java.time.LocalTime
import java.time.temporal.ChronoUnit

fun minutesBetween(duration1: LocalTime, duration2: LocalTime): Duration = Duration.ofMinutes(ChronoUnit.MINUTES.between(duration1, duration2))
