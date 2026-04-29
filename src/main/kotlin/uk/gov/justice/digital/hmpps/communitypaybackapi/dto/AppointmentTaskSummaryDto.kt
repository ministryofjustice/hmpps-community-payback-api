package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.util.UUID

data class AppointmentTaskSummaryDto(
  @param:Schema(description = "The unique identifier for the appointment task", example = "550e8400-e29b-41d4-a716-446655440000")
  val taskId: UUID,
  @param:Schema(description = "Deprecated: use top-level properties of this response instead.\n\nSummary details of the appointment associated with this task", deprecated = true)
  val appointment: AppointmentSummaryDto,
  @param:Schema(description = "")
  val offender: OffenderDto,
  @param:Schema(description = "The date of the appointment.")
  val date: LocalDate? = null,
  @param:Schema(description = "The name of the project attended in this appointment.")
  val projectTypeName: String? = null,
)
