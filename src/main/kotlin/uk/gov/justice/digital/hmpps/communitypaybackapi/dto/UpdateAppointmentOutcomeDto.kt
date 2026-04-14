package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import tools.jackson.databind.annotation.JsonDeserialize
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.SanitizingStringDeserializer
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

data class UpdateAppointmentOutcomeDto(
  @param:Schema(description = "Delius ID of the appointment to update")
  val deliusId: Long,
  @param:Schema(description = "The version of the appointment retrieved from delius this update is being applied to")
  val deliusVersionToUpdate: UUID,
  @param:Schema(description = "If not defined the date will not be modified. Optionality on this field will be removed in the future")
  val date: LocalDate? = null,
  @param:Schema(example = "09:00", description = "The start local time of the appointment", pattern = "^([0-1][0-9]|2[0-3]):[0-5][0-9]$")
  override val startTime: LocalTime,
  @param:Schema(example = "14:00", description = "The end local time of the appointment", pattern = "^([0-1][0-9]|2[0-3]):[0-5][0-9]$")
  override val endTime: LocalTime,
  override val contactOutcomeCode: String?,
  override val attendanceData: AttendanceDataDto?,
  @Deprecated("Setting specific enforcement data is not supported")
  @param:Schema(description = "Setting specific enforcement data is not supported", deprecated = true)
  val enforcementData: EnforcementDto?,
  val supervisorOfficerCode: String,
  @field:JsonDeserialize(using = SanitizingStringDeserializer::class)
  override val notes: String? = null,
  val alertActive: Boolean?,
  @param:Schema(description = "If the corresponding delius contact should be marked as sensitive")
  val sensitive: Boolean?,
) : AppointmentCommandDto {

  fun resolveDate(existingAppointment: AppointmentDto) = date ?: existingAppointment.date

  companion object
}
