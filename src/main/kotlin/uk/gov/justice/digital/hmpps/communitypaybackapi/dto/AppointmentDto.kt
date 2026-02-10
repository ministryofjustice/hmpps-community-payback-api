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
  @Deprecated("Retrieve project information from GET project instead")
  @param:Schema(description = "Retrieve project information from GET project instead. Whilst marked as optional to support deprecation, this will always be populated.", deprecated = true)
  val projectName: String?,
  val projectCode: String,
  @Deprecated("Retrieve project information from GET project instead")
  @param:Schema(description = "Retrieve project information from GET project instead. Whilst marked as optional to support deprecation, this will always be populated.", deprecated = true)
  val projectTypeName: String?,
  @Deprecated("Retrieve project information from GET project instead")
  @param:Schema(description = "Retrieve project information from GET project instead. Whilst marked as optional to support deprecation, this will always be populated.", deprecated = true)
  val projectTypeCode: String?,
  @Deprecated("Retrieve project information from GET project instead")
  @param:Schema(description = "Retrieve project information from GET project instead. Whilst marked as optional to support deprecation, this will always be populated.", deprecated = true)
  val projectType: ProjectTypeDto?,
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
