package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

data class UpdateAppointmentOutcomesDto(
  val updates: List<UpdateAppointmentOutcomeDto>,
) {
  companion object
}
