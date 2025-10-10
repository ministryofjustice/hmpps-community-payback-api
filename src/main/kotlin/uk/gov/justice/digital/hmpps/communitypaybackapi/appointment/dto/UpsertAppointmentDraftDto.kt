package uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Request body for creating/updating an appointment draft.
 */
data class UpsertAppointmentDraftDto(
  val crn: String,
  val projectName: String,
  val projectCode: String,
  val projectTypeId: UUID,
  val supervisingTeamCode: String? = null,
  val appointmentDate: LocalDate,
  @param:Schema(example = "09:00")
  val startTime: LocalTime,
  @param:Schema(example = "14:00")
  val endTime: LocalTime,
  val attendanceData: AttendanceDataDto? = null,
  val enforcementData: EnforcementDto? = null,
  val notes: String? = null,
  val deliusLastUpdatedAt: OffsetDateTime,
)
