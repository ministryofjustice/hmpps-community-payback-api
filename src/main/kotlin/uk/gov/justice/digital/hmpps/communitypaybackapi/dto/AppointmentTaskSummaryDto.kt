package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

data class AppointmentTaskSummaryDto(
  @param:Schema(description = "The unique identifier for the appointment task", example = "550e8400-e29b-41d4-a716-446655440000")
  val taskId: UUID,
)
