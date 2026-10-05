package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import com.github.tomakehurst.wiremock.client.WireMock.aMultipart
import com.github.tomakehurst.wiremock.client.WireMock.binaryEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.notFound
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.serverError
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.web.reactive.function.BodyInserters

class AdminAppointmentDocumentIT : IntegrationTestBase() {
  private val upstreamPath = "/community-payback-and-delius/appointments/101/documents"
  private val endpoint = "/admin/appointments/101/documents"
  private val fileBytes = byteArrayOf(0, 1, 127, -128, -1)

  @Test
  fun `uploads binary file and returns document metadata`() {
    stubFor(
      post(urlEqualTo(upstreamPath))
        .withMultipartRequestBody(
          aMultipart().withName("file")
            .withHeader("Content-Disposition", containing("filename=\"evidence.pdf\""))
            .withHeader("Content-Type", equalTo("application/pdf"))
            .withBody(binaryEqualTo(fileBytes)),
        )
        .willReturn(okJson("""{"documentId":42,"filename":"evidence.pdf","alfrescoId":"alfresco-id"}""")),
    )

    webTestClient.post().uri(endpoint).addAdminUiAuthHeader()
      .body(upload()).exchange().expectStatus().isOk
      .expectBody()
      .jsonPath("$.documentId").isEqualTo(42)
      .jsonPath("$.filename").isEqualTo("evidence.pdf")
      .jsonPath("$.alfrescoId").isEqualTo("alfresco-id")
    verify(1, postRequestedFor(urlEqualTo(upstreamPath)))
  }

  @Test
  fun `requires authentication`() {
    webTestClient.post().uri(endpoint).body(upload()).exchange().expectStatus().isUnauthorized
    verify(0, postRequestedFor(urlEqualTo(upstreamPath)))
  }

  @Test
  fun `rejects supervisor role`() {
    webTestClient.post().uri(endpoint).addSupervisorUiAuthHeader()
      .body(upload()).exchange().expectStatus().isForbidden
    verify(0, postRequestedFor(urlEqualTo(upstreamPath)))
  }

  @Test
  fun `rejects missing file`() {
    val body = MultipartBodyBuilder().apply { part("other", "value") }
    webTestClient.post().uri(endpoint).addAdminUiAuthHeader()
      .body(BodyInserters.fromMultipartData(body.build())).exchange().expectStatus().isBadRequest
    verify(0, postRequestedFor(urlEqualTo(upstreamPath)))
  }

  @ParameterizedTest
  @ValueSource(strings = ["file.exe", "file.mp4", "file.wav", "file.wma", "file.aac", "file", "file.pdf.exe"])
  fun `rejects unsupported document formats without calling Delius`(filename: String) {
    webTestClient.post().uri(endpoint).addAdminUiAuthHeader()
      .body(upload(filename = filename)).exchange().expectStatus().isBadRequest
      .expectBody().jsonPath("$.userMessage")
      .value<String> { org.assertj.core.api.Assertions.assertThat(it).contains("Unsupported document format") }
    verify(0, postRequestedFor(urlEqualTo(upstreamPath)))
  }

  @Test
  fun `rejects empty file`() {
    webTestClient.post().uri(endpoint).addAdminUiAuthHeader()
      .body(upload(byteArrayOf())).exchange().expectStatus().isBadRequest
    verify(0, postRequestedFor(urlEqualTo(upstreamPath)))
  }

  @Test
  fun `rejects invalid appointment ID`() {
    webTestClient.post().uri("/admin/appointments/invalid/documents").addAdminUiAuthHeader()
      .body(upload()).exchange().expectStatus().isBadRequest
    verify(0, postRequestedFor(urlEqualTo(upstreamPath)))
  }

  @Test
  fun `returns not found for unknown appointment`() {
    stubFor(post(urlEqualTo(upstreamPath)).willReturn(notFound()))
    webTestClient.post().uri(endpoint).addAdminUiAuthHeader()
      .body(upload()).exchange().expectStatus().isNotFound
    verify(1, postRequestedFor(urlEqualTo(upstreamPath)))
  }

  @Test
  fun `reports upstream failure without retrying upload`() {
    stubFor(post(urlEqualTo(upstreamPath)).willReturn(serverError()))
    webTestClient.post().uri(endpoint).addAdminUiAuthHeader()
      .body(upload()).exchange().expectStatus().is5xxServerError
    verify(1, postRequestedFor(urlEqualTo(upstreamPath)))
  }

  @Test
  fun `documents multipart request and upload response`() {
    val operation = "$.paths['/admin/appointments/{deliusAppointmentId}/documents'].post"
    webTestClient.get().uri("/v3/api-docs").exchange().expectStatus().isOk
      .expectBody()
      .jsonPath("$operation.requestBody.content['multipart/form-data'].schema.properties.file.format").isEqualTo("binary")
      .jsonPath("$operation.responses['200'].content['application/json'].schema['${'$'}ref']")
      .isEqualTo("#/components/schemas/DocumentUploadResponseDto")
  }

  private fun upload(bytes: ByteArray = fileBytes, filename: String = "evidence.pdf") = BodyInserters.fromMultipartData(
    MultipartBodyBuilder().apply {
      part("file", bytes).filename(filename).contentType(MediaType.APPLICATION_PDF)
    }.build(),
  )
}
