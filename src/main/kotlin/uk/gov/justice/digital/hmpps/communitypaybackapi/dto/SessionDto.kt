package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import java.time.LocalDate
import java.time.LocalTime

data class SessionDto(
  val projectName: String,
  val projectCode: String,
  val projectLocation: String,
  val date: LocalDate,
  val startTime: LocalTime,
  val endTime: LocalTime,
  val appointmentSummaries: List<AppointmentSummaryDto>,
)
