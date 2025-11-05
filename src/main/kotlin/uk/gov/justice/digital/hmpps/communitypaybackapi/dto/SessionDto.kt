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
  val location: LocationDto,
  val date: LocalDate,
  @Deprecated("Will be removed")
  @param:Schema(description = "Deprecated", deprecated = true)
  val startTime: LocalTime,
  @Deprecated("Will be removed")
  @param:Schema(description = "Deprecated", deprecated = true)
  val endTime: LocalTime,
  val appointmentSummaries: List<AppointmentSummaryDto>,
)
