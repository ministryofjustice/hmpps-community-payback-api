package uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.http.HttpHeaders
import java.time.LocalDateTime
import java.time.ZoneOffset

object HmppsAuthMockServer {

  fun stubGrantToken() {
    WireMock.stubFor(
      post(urlEqualTo("/auth/oauth/token"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              """
            {
              "token_type": "bearer",
              "access_token": "ABCDE",
              "expires_in": ${LocalDateTime.now().plusHours(2).toEpochSecond(ZoneOffset.UTC)}
            }
              """.trimIndent(),
            ),
        ),
    )
  }

  fun stubHealthPing(status: Int) {
    WireMock.stubFor(
      get("/auth/health/ping").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(if (status == 200) """{"status":"UP"}""" else """{"status":"DOWN"}""")
          .withStatus(status),
      ),
    )
  }
}
