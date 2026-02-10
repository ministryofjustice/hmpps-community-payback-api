package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import io.swagger.v3.oas.annotations.media.Schema

data class ProjectSummaryDto(
  @param:Schema(description = "Project name", example = "Community Garden Maintenance")
  val projectName: String,
  @param:Schema(description = "Project code", example = "123")
  val projectCode: String,
  @param:Schema(description = "Project location")
  val location: LocationDto,
  @param:Schema(description = "Number of appointments overdue", example = "2")
  val numberOfAppointmentsOverdue: Int,
  @param:Schema(description = "Oldest overdue appointment (in days)", example = "3")
  val oldestOverdueAppointmentInDays: Int,
)
