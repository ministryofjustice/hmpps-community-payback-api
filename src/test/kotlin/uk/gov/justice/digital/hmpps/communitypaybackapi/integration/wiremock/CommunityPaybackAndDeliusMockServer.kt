package uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAllocations
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProviderSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProviderTeamSummaries

object CommunityPaybackAndDeliusMockServer {

  val objectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

  fun providers(
    providers: ProviderSummaries,
  ) {
    WireMock.stubFor(
      get("/community-payback-and-delius/providers").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(objectMapper.writer().writeValueAsString(providers)),
      ),
    )
  }

  fun providerTeams(
    providerTeams: ProviderTeamSummaries,
  ) {
    WireMock.stubFor(
      get("/community-payback-and-delius/provider-teams").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(objectMapper.writer().writeValueAsString(providerTeams)),
      ),
    )
  }

  fun projectAllocations(
    projectAllocations: ProjectAllocations,
  ) {
    WireMock.stubFor(
      get("/community-payback-and-delius/project-allocations?startDate=01/09/2025&endDate=07/09/2025&teamId=1")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(objectMapper.writer().writeValueAsString(projectAllocations)),
        ),
    )

    // Add a more flexible stub for different parameters
    WireMock.stubFor(
      get(WireMock.urlPathEqualTo("/community-payback-and-delius/project-allocations"))
        .withQueryParam("startDate", WireMock.matching(".+"))
        .withQueryParam("endDate", WireMock.matching(".+"))
        .withQueryParam("teamId", WireMock.matching("\\d+"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(objectMapper.writer().writeValueAsString(projectAllocations)),
        ),
    )
  }
}
