package uk.gov.justice.digital.hmpps.communitypaybackapi.integration.config

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.verify
import com.github.tomakehurst.wiremock.stubbing.Scenario
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCreateAppointments
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProviderSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.IntegrationTestBase

@TestPropertySource(
  properties = [
    "client.community-payback-and-delius.timeout=500ms",
  ],
)
class WebClientRetryIT : IntegrationTestBase() {

  @Autowired
  private lateinit var communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient

  @Test
  fun `GET request is retried on timeout`() {
    val username = "some-user"
    val scenarioName = "Retry Scenario"

    // 1st attempt: timeout
    stubFor(
      get(WireMock.urlPathEqualTo("/community-payback-and-delius/providers"))
        .withQueryParam("username", WireMock.equalTo(username))
        .inScenario(scenarioName)
        .whenScenarioStateIs(Scenario.STARTED)
        .willReturn(aResponse().withFixedDelay(1000)) // longer than 500ms timeout
        .willSetStateTo("First Timeout"),
    )

    // 2nd attempt: timeout
    stubFor(
      get(WireMock.urlPathEqualTo("/community-payback-and-delius/providers"))
        .withQueryParam("username", WireMock.equalTo(username))
        .inScenario(scenarioName)
        .whenScenarioStateIs("First Timeout")
        .willReturn(aResponse().withFixedDelay(1000))
        .willSetStateTo("Second Timeout"),
    )

    // 3rd attempt: success
    stubFor(
      get(WireMock.urlPathEqualTo("/community-payback-and-delius/providers"))
        .withQueryParam("username", WireMock.equalTo(username))
        .inScenario(scenarioName)
        .whenScenarioStateIs("Second Timeout")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody("""{"providers": []}"""),
        ),
    )

    val response = communityPaybackAndDeliusClient.getProviders(username)

    assertThat(response).isEqualTo(NDProviderSummaries(emptyList()))

    verify(3, getRequestedFor(WireMock.urlPathEqualTo("/community-payback-and-delius/providers")))
  }

  @Test
  fun `GET request eventually fails after max retries`() {
    val username = "some-user"

    stubFor(
      get(WireMock.urlPathEqualTo("/community-payback-and-delius/providers"))
        .withQueryParam("username", WireMock.equalTo(username))
        .willReturn(aResponse().withFixedDelay(1000)),
    )

    assertThrows<Exception> {
      communityPaybackAndDeliusClient.getProviders(username)
    }

    verify(4, getRequestedFor(WireMock.urlPathEqualTo("/community-payback-and-delius/providers")))
  }

  @Test
  fun `POST request is NOT retried on timeout`() {
    val projectCode = "P1"

    stubFor(
      post("/community-payback-and-delius/projects/$projectCode/appointments")
        .willReturn(aResponse().withFixedDelay(1000)),
    )

    assertThrows<Exception> {
      communityPaybackAndDeliusClient.createAppointments(projectCode, NDCreateAppointments(emptyList()))
    }

    verify(1, postRequestedFor(urlEqualTo("/community-payback-and-delius/projects/$projectCode/appointments")))
  }
}
