package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

data class CreateAppointmentsDto(
  val projectCode: String,
  val appointments: List<CreateAppointmentDto>,
)
