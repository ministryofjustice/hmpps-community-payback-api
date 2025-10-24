package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

data class UpdateAppointmentsOutcomesResultDto(
  val results: List<UpdateAppointmentOutcomeDto>,
)

data class UpdateAppointmentOutcomeResultDto(
  val id: Long,
  val type: UpdateAppointmentOutcomeResultType,
)

enum class UpdateAppointmentOutcomeResultType {
  SUCCESS,
  NOT_FOUND,
  VERSION_CONFLICT,
  SERVER_ERROR,
}
