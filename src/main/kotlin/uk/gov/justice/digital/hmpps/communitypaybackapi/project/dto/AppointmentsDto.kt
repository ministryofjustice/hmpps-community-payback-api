package uk.gov.justice.digital.hmpps.communitypaybackapi.project.dto

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.dto.OffenderDto

// DA: move into appointment package structure?
data class AppointmentDto(
  val id: Long,
  @param:Schema(description = "Project name", example = "Community Garden Maintenance")
  val projectName: String,
  @param:Schema(description = "How many community payback minutes the offender is required to complete", example = "2400")
  val requirementMinutes: Int,
  @param:Schema(description = "How many community payback minutes the offender has completed to date", example = "480")
  val completedMinutes: Int,
  val offender: OffenderDto,
)

data class AppointmentsDto(

  val appointments: List<AppointmentDto>,
)
