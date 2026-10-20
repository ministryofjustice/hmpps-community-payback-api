package uk.gov.justice.digital.hmpps.communitypaybackapi.integration.config

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.notFound
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.IntegrationTestBase
import kotlin.random.Random

@TestPropertySource(
  properties = [
    "client.log-downstream-error-responses=true",
  ],
)
class WebClientErrorLoggingIT : IntegrationTestBase() {
  private val upstreamPath = "/community-payback-and-delius/appointments/101/documents/42"
  private val endpoint = "/admin/appointments/101/documents/42"

  @Test
  fun `successful binary responses larger than the in-memory codec limit are passed through unchanged`() {
    val largeFile = Random(1).nextBytes(11 * 1024 * 1024)
    stubFor(
      get(urlEqualTo(upstreamPath)).willReturn(
        aResponse().withStatus(200).withHeader("Content-Type", "application/pdf").withBody(largeFile),
      ),
    )

    webTestClient.mutate().codecs { it.defaultCodecs().maxInMemorySize(largeFile.size * 2) }.build()
      .get().uri(endpoint).addAdminUiAuthHeader()
      .exchange().expectStatus().isOk
      .expectBody(ByteArray::class.java).isEqualTo(largeFile)
  }

  @Test
  fun `error responses are still handled after being logged`() {
    stubFor(get(urlEqualTo(upstreamPath)).willReturn(notFound().withBody("Document not found")))

    webTestClient.get().uri(endpoint).addAdminUiAuthHeader()
      .exchange().expectStatus().isNotFound
      .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
  }
}
