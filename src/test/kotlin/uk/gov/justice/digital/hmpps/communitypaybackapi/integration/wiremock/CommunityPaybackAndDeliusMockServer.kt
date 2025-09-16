package uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAllocations
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectTypes
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
    providerId: Long,
    providerTeams: ProviderTeamSummaries,
  ) {
    WireMock.stubFor(
      get("/community-payback-and-delius/provider-teams?providerId=$providerId").willReturn(
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
      get("/community-payback-and-delius/project-allocations?startDate=2025-01-09&endDate=2025-07-09&teamId=999")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(objectMapper.writer().writeValueAsString(projectAllocations)),
        ),
    )
  }

  fun projectTypes(
    projectTypes: ProjectTypes,
  ) {
    WireMock.stubFor(
      get("/community-payback-and-delius/references/project-types")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(objectMapper.writer().writeValueAsString(projectTypes)),
        ),
    )
  }
}
