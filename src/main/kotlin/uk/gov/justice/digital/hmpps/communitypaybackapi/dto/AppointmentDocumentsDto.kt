package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import io.swagger.v3.oas.annotations.media.Schema

data class AppointmentDocumentsDto(
  @param:Schema(description = "List of documents attached to the appointment")
  val documents: List<AppointmentDocumentDto>,
)

data class AppointmentDocumentDto(
  val documentId: Long,
  val filename: String,
)
