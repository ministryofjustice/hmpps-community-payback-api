package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.core.io.Resource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDDocumentUploadResponse
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.DocumentUploadResponseDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.BadRequestException
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentDocumentService

class AppointmentDocumentServiceTest {
  private val client = mockk<CommunityPaybackAndDeliusClient>()
  private val service = AppointmentDocumentService(client)
  private val bytes = byteArrayOf(0, 1, 127, -128, -1)

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = ["application/pdf"])
  fun `forwards file and maps response`(contentType: String?) {
    val part = slot<HttpEntity<Resource>>()
    every { client.uploadAppointmentDocument(101, capture(part)) } returns NDDocumentUploadResponse(42, "evidence.pdf", "alfresco-id")
    val file = MockMultipartFile("file", "evidence.pdf", contentType?.takeIf { it.isNotEmpty() }, bytes)

    assertThat(service.uploadDocument(101, file)).isEqualTo(DocumentUploadResponseDto(42, "evidence.pdf", "alfresco-id"))
    assertThat(part.captured.body!!.filename).isEqualTo("evidence.pdf")
    assertThat(part.captured.body!!.inputStream.readAllBytes()).isEqualTo(bytes)
    assertThat(part.captured.headers.contentType).isEqualTo(if (contentType.isNullOrEmpty()) MediaType.APPLICATION_OCTET_STREAM else MediaType.APPLICATION_PDF)
    verify(exactly = 1) { client.uploadAppointmentDocument(101, any()) }
  }

  @ParameterizedTest
  @ValueSource(strings = ["*/*", "application/*", "application/*+json", "invalid"])
  fun `rejects invalid content type without calling Delius`(contentType: String) {
    assertThatThrownBy { service.uploadDocument(101, MockMultipartFile("file", "test.pdf", contentType, bytes)) }
      .isInstanceOf(BadRequestException::class.java)
    verify { client wasNot Called }
  }

  @ParameterizedTest
  @ValueSource(
    strings = [
      "doc", "docx", "rtf", "txt", "dot", "dotm", "docm", "odt", "xml", "wpd", "wri", "wps",
      "xls", "xlsb", "xlsx", "csv", "pdf", "bmp", "jpg", "jpeg", "gif", "png", "m4a", "flac", "mp3", "PDF", "DoCx",
    ],
  )
  fun `accepts supported extensions case insensitively`(extension: String) {
    val filename = "evidence.$extension"
    every { client.uploadAppointmentDocument(101, any()) } returns NDDocumentUploadResponse(42, filename, "alfresco-id")
    assertThat(service.uploadDocument(101, MockMultipartFile("file", filename, null, bytes))!!.filename).isEqualTo(filename)
    verify(exactly = 1) { client.uploadAppointmentDocument(101, any()) }
  }

  @ParameterizedTest
  @ValueSource(strings = ["file.exe", "file.mp4", "file.wav", "file.wma", "file.aac", "file.zip", "pdf", "file.", "file.pdf.exe", "file.pdf "])
  fun `rejects unsupported extensions even with PDF content type`(filename: String) {
    assertThatThrownBy { service.uploadDocument(101, MockMultipartFile("file", filename, "application/pdf", bytes)) }
      .isInstanceOf(BadRequestException::class.java)
      .hasMessageStartingWith("Unsupported document format.")
    verify { client wasNot Called }
  }

  @Test
  fun `rejects empty file without calling Delius`() {
    assertThatThrownBy { service.uploadDocument(101, MockMultipartFile("file", "empty.pdf", "application/pdf", byteArrayOf())) }
      .isInstanceOf(BadRequestException::class.java)
      .hasMessage("Document must not be empty")
    verify { client wasNot Called }
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = [" "])
  fun `rejects missing filename`(filename: String?) {
    assertThatThrownBy { service.uploadDocument(101, MockMultipartFile("file", filename, null, bytes)) }
      .isInstanceOf(BadRequestException::class.java)
      .hasMessage("Document must have a filename")
    verify { client wasNot Called }
  }

  @Test
  fun `returns null when appointment not found`() {
    every { client.uploadAppointmentDocument(101, any()) } throws WebClientResponseException.create(404, "Not found", HttpHeaders(), byteArrayOf(), null)
    assertThat(service.uploadDocument(101, MockMultipartFile("file", "test.pdf", null, bytes))).isNull()
  }

  @Test
  fun `propagates unexpected upstream failures`() {
    val failure = WebClientResponseException.create(500, "Failure", HttpHeaders(), byteArrayOf(), null)
    every { client.uploadAppointmentDocument(101, any()) } throws failure
    assertThatThrownBy { service.uploadDocument(101, MockMultipartFile("file", "test.pdf", null, bytes)) }.isSameAs(failure)
    verify(exactly = 1) { client.uploadAppointmentDocument(101, any()) }
  }

  @Test
  fun `deletes document`() {
    every { client.deleteAppointmentDocument(101, 42) } returns Unit
    assertThat(service.deleteDocument(101, 42)).isTrue()
    verify(exactly = 1) { client.deleteAppointmentDocument(101, 42) }
  }

  @Test
  fun `returns false when appointment or document not found`() {
    every { client.deleteAppointmentDocument(101, 42) } throws WebClientResponseException.create(404, "Not found", HttpHeaders(), byteArrayOf(), null)
    assertThat(service.deleteDocument(101, 42)).isFalse()
  }

  @Test
  fun `propagates unexpected upstream deletion failures`() {
    val failure = WebClientResponseException.create(500, "Failure", HttpHeaders(), byteArrayOf(), null)
    every { client.deleteAppointmentDocument(101, 42) } throws failure
    assertThatThrownBy { service.deleteDocument(101, 42) }.isSameAs(failure)
    verify(exactly = 1) { client.deleteAppointmentDocument(101, 42) }
  }
}
