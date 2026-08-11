package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto

import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreatedAppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import java.util.UUID

fun CreatedAppointmentDto.Companion.full() = CreatedAppointmentDto(
  id = UUID.randomUUID(),
  deliusId = Long.random(),
)
