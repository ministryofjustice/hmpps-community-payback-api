package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

data class DeliusAppointmentIdDto(
  val projectCode: String,
  val deliusAppointmentId: Long,
) {
  override fun toString() = "Project $projectCode, NDelius ID $deliusAppointmentId"
}
