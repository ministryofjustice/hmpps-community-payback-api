package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

data class AppointmentDto(
  val id: Long,
  val communityPaybackId: UUID?,
  val version: UUID,
  val deliusEventNumber: Int,
  val projectName: String,
  val projectCode: String,
  @Deprecated("Use [projectType.name] instead")
  @param:Schema(description = "Deprecated, use projectType.name instead", deprecated = true)
  val projectTypeName: String?,
  @Deprecated("Use [projectType.code] instead")
  @param:Schema(description = "Deprecated, use projectType.code instead", deprecated = true)
  val projectTypeCode: String?,
  val projectType: ProjectTypeDto,
  val offender: OffenderDto,
  val supervisingTeam: String,
  val supervisingTeamCode: String,
  val providerCode: String,
  val pickUpData: PickUpDataDto?,
  val date: LocalDate,
  val startTime: LocalTime,
  val endTime: LocalTime,
  val contactOutcomeCode: String? = null,
  val attendanceData: AttendanceDataDto?,
  val enforcementData: EnforcementDto?,
  val supervisorOfficerCode: String,
  val notes: String?,
  val sensitive: Boolean?,
  val alertActive: Boolean?,
) {
  companion object
}
