package uk.gov.justice.digital.hmpps.communitypaybackapi.controller.admin

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.multipart.MultipartFile
import uk.gov.justice.digital.hmpps.communitypaybackapi.controller.internal.notFound
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.DocumentUploadResponseDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentDocumentService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@AdminUiController
@RequestMapping("/admin/appointments", produces = [MediaType.APPLICATION_JSON_VALUE])
class AdminAppointmentDocumentController(
  private val appointmentDocumentService: AppointmentDocumentService,
) {
  @PostMapping("/{deliusAppointmentId}/documents", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
  @Operation(
    description = "Upload a document for an appointment. Supported extensions (case-insensitive): " +
      "doc, docx, rtf, txt, dot, dotm, docm, odt, xml, wpd, wri, wps, xls, xlsb, xlsx, csv, pdf, bmp, jpg, jpeg, gif, png, m4a, flac, mp3.",
    responses = [
      ApiResponse(responseCode = "200", description = "Document uploaded successfully"),
      ApiResponse(responseCode = "400", description = "Missing or empty file, unsupported extension, or invalid content type", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
      ApiResponse(responseCode = "404", description = "Appointment not found", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    ],
  )
  fun uploadDocument(
    @PathVariable deliusAppointmentId: Long,
    @RequestPart("file") file: MultipartFile,
  ): DocumentUploadResponseDto = appointmentDocumentService.uploadDocument(deliusAppointmentId, file)
    ?: notFound("Appointment", deliusAppointmentId)
}
