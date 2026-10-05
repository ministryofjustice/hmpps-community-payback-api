package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

data class DocumentUploadResponseDto(
  val documentId: Long,
  val filename: String,
  val alfrescoId: String,
)
