package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import java.util.UUID

data class CreatedAppointmentDto(
  val id: UUID,
  val deliusId: Long,
) {
  companion object
}
