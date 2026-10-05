package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.badRequest
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.DocumentUploadResponseDto

@Service
class AppointmentDocumentService(
  private val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
) {
  fun uploadDocument(deliusAppointmentId: Long, file: MultipartFile): DocumentUploadResponseDto? {
    if (file.isEmpty) badRequest("Document must not be empty")
    if (file.originalFilename.isNullOrBlank()) badRequest("Document must have a filename")

    val extension = file.originalFilename!!.substringAfterLast('.', "").lowercase()
    if (extension !in ALLOWED_EXTENSIONS) {
      badRequest("Unsupported document format. Allowed extensions: ${ALLOWED_EXTENSIONS.joinToString(", ")}")
    }

    val mediaType = try {
      file.contentType?.takeIf { it.isNotBlank() }?.let { MediaType.parseMediaType(it) } ?: MediaType.APPLICATION_OCTET_STREAM
    } catch (_: IllegalArgumentException) {
      badRequest("Document content type must be a valid media type")
    }
    if (mediaType.isWildcardType || mediaType.isWildcardSubtype) {
      badRequest("Document content type must not contain wildcards")
    }
    val headers = HttpHeaders().apply {
      contentType = mediaType
    }
    return try {
      communityPaybackAndDeliusClient.uploadAppointmentDocument(deliusAppointmentId, HttpEntity(file.resource, headers))
        .let { DocumentUploadResponseDto(it.documentId, it.filename, it.alfrescoId) }
    } catch (_: WebClientResponseException.NotFound) {
      null
    }
  }
  private companion object {
    val ALLOWED_EXTENSIONS = setOf(
      "doc", "docx", "rtf", "txt", "dot", "dotm", "docm", "odt", "xml", "wpd", "wri", "wps",
      "xls", "xlsb", "xlsx", "csv", "pdf", "bmp", "jpg", "jpeg", "gif", "png", "m4a", "flac", "mp3",
    )
  }
}
