package uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto

import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.dto.ContactOutcomeDto
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.UUID

data class AppointmentDraftDto(
  val id: UUID,
  val appointmentDeliusId: Long,
  val crn: String,
  val projectName: String,
  val projectCode: String,
  val projectTypeId: UUID,
  val projectTypeName: String?,
  val projectTypeCode: String?,
  val supervisingTeamCode: String?,
  val appointmentDate: LocalDate,
  val startTime: LocalTime,
  val endTime: LocalTime,
  val attendanceData: AttendanceDataDto?,
  val contactOutcome: ContactOutcomeDto?,
  val enforcementData: EnforcementDto?,
  val notes: String?,
  val deliusLastUpdatedAt: OffsetDateTime,
  val createdAt: OffsetDateTime,
  val updatedAt: OffsetDateTime,
)
