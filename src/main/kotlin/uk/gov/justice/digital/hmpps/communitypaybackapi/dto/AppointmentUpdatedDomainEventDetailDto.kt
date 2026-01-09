package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalTime
import java.util.UUID

data class AppointmentUpdatedDomainEventDetailDto(
  val id: UUID,
  val appointmentDeliusId: Long,
  val crn: String,
  val deliusEventNumber: Int,
  @param:Schema(example = "09:00", description = "The start local time of the appointment", pattern = "^([0-1][0-9]|2[0-3]):[0-5][0-9]$")
  val startTime: LocalTime,
  @param:Schema(example = "09:00", description = "The end local time of the appointment", pattern = "^([0-1][0-9]|2[0-3]):[0-5][0-9]$")
  val endTime: LocalTime,
  val contactOutcomeCode: String? = null,
  val supervisorOfficerCode: String? = null,
  val notes: String? = null,
  val hiVisWorn: Boolean? = null,
  val workedIntensively: Boolean? = null,
  val penaltyMinutes: Long? = null,
  val minutesCredited: Long? = null,
  val workQuality: AppointmentWorkQualityDto? = null,
  val behaviour: AppointmentBehaviourDto? = null,
)
