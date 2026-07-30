package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto

import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomesDto

fun UpdateAppointmentOutcomesDto.Companion.valid() = UpdateAppointmentOutcomesDto(
  updates = listOf(UpdateAppointmentDto.valid()),
)
