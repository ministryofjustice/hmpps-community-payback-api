package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class SessionDto(
  val projectName: String,
  val projectCode: String,
  @Deprecated("Use [location] instead")
  @param:Schema(description = "Deprecated, use the structured location instead", deprecated = true)
  val projectLocation: String?,
  val location: LocationDto,
  val date: LocalDate,
  val appointmentSummaries: List<AppointmentSummaryDto>,
)
