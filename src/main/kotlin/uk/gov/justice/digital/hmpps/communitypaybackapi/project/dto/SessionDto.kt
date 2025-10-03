package uk.gov.justice.digital.hmpps.communitypaybackapi.project.dto

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.dto.OffenderDto
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

data class AppointmentSummaryDto(
  val id: Long,
  @param:Schema(
    description = "How many community payback minutes the offender is required to complete",
    example = "2400",
  )
  val requirementMinutes: Int,
  @param:Schema(description = "How many community payback minutes the offender has completed to date", example = "480")
  val completedMinutes: Int,
  val offender: OffenderDto,
)
