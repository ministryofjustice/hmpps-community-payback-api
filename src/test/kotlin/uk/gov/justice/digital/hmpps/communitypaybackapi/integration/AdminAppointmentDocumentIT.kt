package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import com.github.tomakehurst.wiremock.client.WireMock.aMultipart
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.binaryEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.delete
import com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.notFound
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.serverError
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import com.github.tomakehurst.wiremock.client.WireMock.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.web.reactive.function.BodyInserters
import kotlin.random.Random

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
        .willReturn(okJson("""{"documentId":42,"filename":"evidence.pdf"}""")),
    )

    webTestClient.post().uri(endpoint).addAdminUiAuthHeader()
      .body(upload()).exchange().expectStatus().isOk
      .expectBody()
      .jsonPath("$.documentId").isEqualTo(42)
      .jsonPath("$.filename").isEqualTo("evidence.pdf")
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

  @Test
  fun `lists documents for appointment`() {
    stubFor(
      get(urlEqualTo(upstreamPath))
        .willReturn(okJson("""{"content":[{"documentId":42,"filename":"evidence.pdf"},{"documentId":43,"filename":"photo.jpg"}]}""")),
    )
    webTestClient.get().uri(endpoint).addAdminUiAuthHeader()
      .exchange().expectStatus().isOk
      .expectBody()
      .jsonPath("$.documents.length()").isEqualTo(2)
      .jsonPath("$.documents[0].documentId").isEqualTo(42)
      .jsonPath("$.documents[0].filename").isEqualTo("evidence.pdf")
      .jsonPath("$.documents[1].documentId").isEqualTo(43)
      .jsonPath("$.documents[1].filename").isEqualTo("photo.jpg")
    verify(1, getRequestedFor(urlEqualTo(upstreamPath)))
  }

  @Test
  fun `lists no documents for appointment without documents`() {
    stubFor(get(urlEqualTo(upstreamPath)).willReturn(okJson("""{"content":[]}""")))
    webTestClient.get().uri(endpoint).addAdminUiAuthHeader()
      .exchange().expectStatus().isOk
      .expectBody().jsonPath("$.documents.length()").isEqualTo(0)
  }

  @Test
  fun `listing requires authentication`() {
    webTestClient.get().uri(endpoint).exchange().expectStatus().isUnauthorized
    verify(0, getRequestedFor(urlEqualTo(upstreamPath)))
  }

  @Test
  fun `listing rejects supervisor role`() {
    webTestClient.get().uri(endpoint).addSupervisorUiAuthHeader()
      .exchange().expectStatus().isForbidden
    verify(0, getRequestedFor(urlEqualTo(upstreamPath)))
  }

  @Test
  fun `listing rejects invalid appointment ID`() {
    webTestClient.get().uri("/admin/appointments/invalid/documents").addAdminUiAuthHeader()
      .exchange().expectStatus().isBadRequest
    verify(0, getRequestedFor(urlMatching(".*documents.*")))
  }

  @Test
  fun `listing returns not found for unknown appointment`() {
    stubFor(get(urlEqualTo(upstreamPath)).willReturn(notFound()))
    webTestClient.get().uri(endpoint).addAdminUiAuthHeader()
      .exchange().expectStatus().isNotFound
    verify(1, getRequestedFor(urlEqualTo(upstreamPath)))
  }

  @Test
  fun `deletes document and returns empty success response`() {
    stubFor(delete(urlEqualTo("$upstreamPath/42")).willReturn(ok()))
    webTestClient.delete().uri("$endpoint/42").addAdminUiAuthHeader()
      .exchange().expectStatus().isOk.expectBody().isEmpty
    verify(1, deleteRequestedFor(urlEqualTo("$upstreamPath/42")))
  }

  @Test
  fun `deletion requires authentication`() {
    webTestClient.delete().uri("$endpoint/42").exchange().expectStatus().isUnauthorized
    verify(0, deleteRequestedFor(urlEqualTo("$upstreamPath/42")))
  }

  @Test
  fun `deletion rejects supervisor role`() {
    webTestClient.delete().uri("$endpoint/42").addSupervisorUiAuthHeader()
      .exchange().expectStatus().isForbidden
    verify(0, deleteRequestedFor(urlEqualTo("$upstreamPath/42")))
  }

  @ParameterizedTest
  @ValueSource(strings = ["invalid/documents/42", "101/documents/invalid"])
  fun `deletion rejects invalid IDs`(path: String) {
    webTestClient.delete().uri("/admin/appointments/$path").addAdminUiAuthHeader()
      .exchange().expectStatus().isBadRequest
    verify(0, deleteRequestedFor(urlMatching(".*documents.*")))
  }

  @Test
  fun `deletion returns not found for unknown appointment or document`() {
    stubFor(delete(urlEqualTo("$upstreamPath/42")).willReturn(notFound()))
    webTestClient.delete().uri("$endpoint/42").addAdminUiAuthHeader()
      .exchange().expectStatus().isNotFound
    verify(1, deleteRequestedFor(urlEqualTo("$upstreamPath/42")))
  }

  @Test
  fun `reports upstream deletion failure without retrying`() {
    stubFor(delete(urlEqualTo("$upstreamPath/42")).willReturn(serverError()))
    webTestClient.delete().uri("$endpoint/42").addAdminUiAuthHeader()
      .exchange().expectStatus().is5xxServerError
    verify(1, deleteRequestedFor(urlEqualTo("$upstreamPath/42")))
  }

  @Test
  fun `downloads document content with type, length and filename from Delius`() {
    stubFor(
      get(urlEqualTo("$upstreamPath/42")).willReturn(
        aResponse().withStatus(200)
          .withHeader("Content-Type", "application/pdf")
          .withHeader("Content-Disposition", """attachment; filename="evidence.pdf"""")
          .withHeader("Content-Length", fileBytes.size.toString())
          .withBody(fileBytes),
      ),
    )

    webTestClient.get().uri("$endpoint/42").addAdminUiAuthHeader()
      .exchange().expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_PDF)
      .expectHeader().contentLength(fileBytes.size.toLong())
      .expectHeader().valueEquals("Content-Disposition", """attachment; filename="evidence.pdf"""")
      .expectBody(ByteArray::class.java).isEqualTo(fileBytes)
    verify(1, getRequestedFor(urlEqualTo("$upstreamPath/42")))
  }

  @Test
  fun `streams documents larger than the in-memory codec limit`() {
    val largeFile = Random(1).nextBytes(11 * 1024 * 1024)
    stubFor(
      get(urlEqualTo("$upstreamPath/42")).willReturn(
        aResponse().withStatus(200).withHeader("Content-Type", "application/pdf").withBody(largeFile),
      ),
    )

    webTestClient.mutate().codecs { it.defaultCodecs().maxInMemorySize(largeFile.size * 2) }.build()
      .get().uri("$endpoint/42").addAdminUiAuthHeader()
      .exchange().expectStatus().isOk
      .expectBody(ByteArray::class.java).isEqualTo(largeFile)
  }

  @Test
  fun `defaults content type and disposition when not provided by Delius`() {
    stubFor(get(urlEqualTo("$upstreamPath/42")).willReturn(aResponse().withStatus(200).withBody(fileBytes)))

    webTestClient.get().uri("$endpoint/42").addAdminUiAuthHeader()
      .exchange().expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_OCTET_STREAM)
      .expectHeader().valueEquals("Content-Disposition", "attachment")
      .expectBody(ByteArray::class.java).isEqualTo(fileBytes)
  }

  @Test
  fun `download accepts requests for the document's content type`() {
    stubFor(
      get(urlEqualTo("$upstreamPath/42")).willReturn(
        aResponse().withStatus(200).withHeader("Content-Type", "application/pdf").withBody(fileBytes),
      ),
    )

    webTestClient.get().uri("$endpoint/42").accept(MediaType.APPLICATION_PDF).addAdminUiAuthHeader()
      .exchange().expectStatus().isOk
      .expectBody(ByteArray::class.java).isEqualTo(fileBytes)
  }

  @Test
  fun `download requires authentication`() {
    webTestClient.get().uri("$endpoint/42").exchange().expectStatus().isUnauthorized
    verify(0, getRequestedFor(urlEqualTo("$upstreamPath/42")))
  }

  @Test
  fun `download rejects supervisor role`() {
    webTestClient.get().uri("$endpoint/42").addSupervisorUiAuthHeader()
      .exchange().expectStatus().isForbidden
    verify(0, getRequestedFor(urlEqualTo("$upstreamPath/42")))
  }

  @ParameterizedTest
  @ValueSource(strings = ["invalid/documents/42", "101/documents/invalid"])
  fun `download rejects invalid IDs`(path: String) {
    webTestClient.get().uri("/admin/appointments/$path").addAdminUiAuthHeader()
      .exchange().expectStatus().isBadRequest
    verify(0, getRequestedFor(urlMatching(".*documents.*")))
  }

  @Test
  fun `download returns not found for unknown appointment or document`() {
    stubFor(get(urlEqualTo("$upstreamPath/42")).willReturn(notFound()))
    webTestClient.get().uri("$endpoint/42").addAdminUiAuthHeader()
      .exchange().expectStatus().isNotFound
      .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
    verify(1, getRequestedFor(urlEqualTo("$upstreamPath/42")))
  }

  @Test
  fun `reports upstream download failure`() {
    stubFor(get(urlEqualTo("$upstreamPath/42")).willReturn(serverError()))
    webTestClient.get().uri("$endpoint/42").addAdminUiAuthHeader()
      .exchange().expectStatus().is5xxServerError
  }

  private fun upload(bytes: ByteArray = fileBytes, filename: String = "evidence.pdf") = BodyInserters.fromMultipartData(
    MultipartBodyBuilder().apply {
      part("file", bytes).filename(filename).contentType(MediaType.APPLICATION_PDF)
    }.build(),
  )
}
