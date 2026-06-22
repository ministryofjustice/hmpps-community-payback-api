package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

data class UpdateAppointmentsOutcomesResultDto(
  val results: List<UpdateAppointmentOutcomeResultDto>,
)

data class UpdateAppointmentOutcomeResultDto(
  val deliusId: Long,
  val result: UpdateAppointmentOutcomeResultType,
  val errorMessage: String? = null,
)

enum class UpdateAppointmentOutcomeResultType {
  SUCCESS,
  NOT_FOUND,
  VERSION_CONFLICT,
  SERVER_ERROR,
  VALIDATION_ERROR,
}
