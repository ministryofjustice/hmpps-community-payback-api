package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import java.time.LocalDate
import java.time.ZoneId

fun LocalDate.atFirstSecondOfDay() = this.atTime(0, 0).atZone(ZoneId.systemDefault()).toOffsetDateTime()
fun LocalDate.atLastSecondOfDay() = this.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toOffsetDateTime()
