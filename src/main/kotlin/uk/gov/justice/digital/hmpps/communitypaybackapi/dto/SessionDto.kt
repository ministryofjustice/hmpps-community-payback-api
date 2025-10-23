package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalTime

data class SessionDto(
  val projectName: String,
  val projectCode: String,
  @Deprecated("Use [location] instead")
  @param:Schema(description = "Deprecated, use the structured location instead", deprecated = true)
  val projectLocation: String,
  val location: ProjectLocationDto,
  val date: LocalDate,
  val startTime: LocalTime,
  val endTime: LocalTime,
  val appointmentSummaries: List<AppointmentSummaryDto>,
)
